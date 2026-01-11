package dev.slne.surf.redis.cache

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.asCache
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.intellij.lang.annotations.Language
import org.redisson.api.RScript
import org.redisson.api.RScriptReactive
import org.redisson.client.RedisException
import org.redisson.client.RedisNoScriptException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

/**
 * Provides Lua scripts for managing a Redis-based cache with indexed sets. The scripts facilitate
 * operations such as upserting values, removing values by ID, and removing values by index.
 *
 * This object leverages Redis data structures such as sets and hashes for efficient data storage
 * and retrieval, and uses Lua scripting to ensure atomicity for multi-step operations. Each script
 * follows a specific protocol for arguments and return values to facilitate interaction with Redis.
 *
 * Scripts included:
 * 1. `UPSERT`: Adds or updates a value in the cache with optional TTL and maintains indexes.
 * 2. `REMOVE_BY_ID`: Removes a value (and its associated indexes) from the cache by its ID.
 * 3. `REMOVE_BY_INDEX`: Removes all values associated with a specific index name and value from the cache.
 */
enum class SimpleSetRedisCacheLuaScripts(val script: String) {

    /**
     * A Lua script string designed to perform an "upsert" operation in Redis. The script handles the insertion or updating
     * of a value, along with maintaining associated indices and metadata, while ensuring expiration policies are applied.
     *
     * - Input parameters via `ARGV`:
     *   1. `prefix`: The prefix used for all keys to create isolation for the dataset.
     *   2. `id`: The unique identifier of the item to upsert.
     *   3. `json`: The serialized JSON representation of the item's value.
     *   4. `ttlMillis`: Time-to-live (TTL) for the value and associated indices in milliseconds.
     *   5. `indexCount`: The number of distinct indices.
     *   Starting from 6, for each index:
     *     - `name`: Name of the index.
     *     - `valueCount`: Number of values for the index.
     *     - `value1..valueN`: The values associated with the index.
     *
     * - Script behavior:
     *   1. The value is inserted or updated with the specified TTL in a dedicated key.
     *   2. The ID is added to a global ID tracking set, maintaining its TTL.
     *   3. For each index:
     *      - Index entries that are no longer valid are removed.
     *      - Index entries for the current state are added or refreshed.
     *      - Metadata regarding the indices is updated for the ID.
     *   4. The script returns:
     *      - Whether the ID was newly added to the global tracking set (`wasNew`: 0/1).
     *      - A list of touched index entries represented as "indexName<0>indexValue".
     */
    @Language("Lua")
    UPSERT(
        """
        -- ARGV:
        -- 1: prefix
        -- 2: id
        -- 3: json
        -- 4: ttlMillis
        -- 5: indexCount
        -- Then for each index:
        --   name, valueCount, value1..valueN
        --
        -- Returns:
        --   [1] wasNew (0/1)  (based on SADD to ids-set)
        --   [2..] touched index entries as "indexName<0>indexValue"
        
        local prefix = ARGV[1]
        local id = ARGV[2]
        local json = ARGV[3]
        local ttl = tonumber(ARGV[4])
        local indexCount = tonumber(ARGV[5])
        local sep = string.char(0)
        
        local idsKey = prefix .. ":__ids__"
        local valKey = prefix .. ":__val__:" .. id
        
        -- Upsert value with TTL
        redis.call('SET', valKey, json, 'PX', ttl)
        
        -- Canonical presence tracking
        local wasNew = redis.call('SADD', idsKey, id)
        redis.call('PEXPIRE', idsKey, ttl)
        
        local touched = {}
        local i = 6
        
        for _ = 1, indexCount do
          local idxName = ARGV[i]; i = i + 1
          local newCount = tonumber(ARGV[i]); i = i + 1
          
          local metaKey = prefix .. ":__meta__:" .. id .. ":" .. idxName
          
          local oldVals = redis.call('SMEMBERS', metaKey)
          local oldMap = {}
          for _, v in ipairs(oldVals) do
            oldMap[v] = true
          end
          
          local newMap = {}
          local newVals = {}
          for _ = 1, newCount do
            local v = ARGV[i]; i = i + 1
            if not newMap[v] then
              newMap[v] = true
              newVals[#newVals + 1] = v
            end
          end
          
          -- Remove from index-sets that are no longer valid
          for _, v in ipairs(oldVals) do
            if not newMap[v] then
              local idxKey = prefix .. ":__idx__:" .. idxName .. ":" .. v
              redis.call('SREM', idxKey, id)
              if redis.call('SCARD', idxKey) == 0 then
                redis.call('UNLINK', idxKey)
              else
                redis.call('PEXPIRE', idxKey, ttl)
              end
              touched[#touched + 1] = idxName .. sep .. v
            end
          end
          
          -- Add/refresh current index-sets
          for _, v in ipairs(newVals) do
            local idxKey = prefix .. ":__idx__:" .. idxName .. ":" .. v
            
            local added = redis.call('SADD', idxKey, id)
            if added == 1 then
              touched[#touched + 1] = idxName .. sep .. v
            end
            redis.call('PEXPIRE', idxKey, ttl)
          end
          
          -- Rewrite meta-set
          redis.call('DEL', metaKey)
          if #newVals > 0 then
            redis.call('SADD', metaKey, unpack(newVals))
          end
          redis.call('PEXPIRE', metaKey, ttl)
        end
        
        local res = { wasNew }
        for _, t in ipairs(touched) do
          res[#res + 1] = t
        end
        return res
    """.trimIndent()
    ),


    /**
     * A Lua script for removing an entry by its ID in a Redis store while updating related indices.
     *
     * The `REMOVE_BY_ID` script is designed to delete a value associated with a given ID,
     * remove the ID from the main set of IDs, and update all associated indices by removing the references
     * to the ID. If an index becomes empty as a result of removing the ID, the index is also deleted.
     *
     * ARGV:
     * 1: The prefix used to construct Redis keys.
     * 2: The ID of the entry to remove.
     * 3: The number of indices to consider for this entry.
     * 4..: The names of the indices to update (one argument for each index).
     *
     * Returns:
     * - [1] `removed`: A flag indicating whether the entry was removed (`1` if removed, `0` otherwise).
     * - [2..] `touched`: A list of index entries in the format `indexName<0>indexValue` that were affected by the removal.
     *
     * The script ensures that all keys and values related to the specified ID are deleted, and
     * any references to the ID in other Redis structures (such as indices) are cleaned up.
     *
     * Key structure:
     * - IDs are stored in a Redis set with a key constructed as `{prefix}:__ids__`.
     * - The value associated with the ID is stored in a key formatted as `{prefix}:__val__:{id}`.
     * - Metadata related to the indices is stored under keys following the pattern `{prefix}:__meta__:{id}:{indexName}`.
     * - Index entries are maintained under keys formatted as `{prefix}:__idx__:{indexName}:{indexValue}`.
     */
    @Language("Lua")
    REMOVE_BY_ID(
        """
        -- ARGV:
        -- 1: prefix
        -- 2: id
        -- 3: indexCount
        -- 4..: indexName1..indexNameN
        --
        -- Returns:
        --   [1] removed (0/1)
        --   [2..] touched index entries as "indexName<0>indexValue"
        
        local prefix = ARGV[1]
        local id = ARGV[2]
        local indexCount = tonumber(ARGV[3])
        local sep = string.char(0)
        
        local idsKey = prefix .. ":__ids__"
        local valKey = prefix .. ":__val__:" .. id
        
        local removedVal = redis.call('UNLINK', valKey)
        local removedId = redis.call('SREM', idsKey, id)
        
        local touched = {}
        local i = 4
        
        for _ = 1, indexCount do
          local idxName = ARGV[i]; i = i + 1
          local metaKey = prefix .. ":__meta__:" .. id .. ":" .. idxName
          
          local vals = redis.call('SMEMBERS', metaKey)
          for _, v in ipairs(vals) do
            local idxKey = prefix .. ":__idx__:" .. idxName .. ":" .. v
            redis.call('SREM', idxKey, id)
            if redis.call('SCARD', idxKey) == 0 then
              redis.call('DEL', idxKey)
            end
            touched[#touched + 1] = idxName .. sep .. v
          end
          
          redis.call('DEL', metaKey)
        end
        
        if redis.call('SCARD', idsKey) == 0 then
          redis.call('DEL', idsKey)
        end
        
        local removed = 0
        if removedVal > 0 or removedId > 0 then
          removed = 1
        end
        
        local res = { removed }
        for _, t in ipairs(touched) do
          res[#res + 1] = t
        end
        return res
    """.trimIndent()
    ),


    /**
     * A Lua script stored as a string constant. It is used for removing entries by a specific index in Redis.
     *
     * The script performs the following operations:
     * - Deletes the values associated with the specified index key.
     * - Removes the IDs from all relevant Redis sets and cleans up meta-data associated with the IDs.
     * - Ensures that any empty index or meta-data keys are also deleted for cleanup purposes.
     *
     * Arguments (ARGV) passed to the script:
     * - ARGV[1]: `prefix` - The key prefix used to identify the data in Redis.
     * - ARGV[2]: `targetIndexName` - The name of the target index to match for deletion.
     * - ARGV[3]: `targetIndexValue` - The value of the target index to match for deletion.
     * - ARGV[4]: `indexCount` - The number of additional indexes to clean up for the IDs being removed.
     * - ARGV[5..]: Names of the additional indexes to clean up.
     *
     * Return value:
     * - The script returns the count of entries removed (`removedCount`).
     */
    @Language("Lua")
    REMOVE_BY_INDEX(
        """
        -- ARGV:
        -- 1: prefix
        -- 2: targetIndexName
        -- 3: targetIndexValue
        -- 4: indexCount
        -- 5..: indexName1..indexNameN
        --
        -- Returns:
        --   removedCount (integer)
        
        local prefix = ARGV[1]
        local targetName = ARGV[2]
        local targetValue = ARGV[3]
        local indexCount = tonumber(ARGV[4])
        
        local idsKey = prefix .. ":__ids__"
        local targetKey = prefix .. ":__idx__:" .. targetName .. ":" .. targetValue
        
        local removedCount = 0
        
        while true do
          local id = redis.call('SPOP', targetKey)
          if not id then break end
          
          redis.call('DEL', prefix .. ":__val__:" .. id)
          redis.call('SREM', idsKey, id)
          
          local j = 5
          for _ = 1, indexCount do
            local idxName = ARGV[j]; j = j + 1
            local metaKey = prefix .. ":__meta__:" .. id .. ":" .. idxName
            local vals = redis.call('SMEMBERS', metaKey)
            
            for _, v in ipairs(vals) do
              local idxKey = prefix .. ":__idx__:" .. idxName .. ":" .. v
              redis.call('SREM', idxKey, id)
               if redis.call('SCARD', idxKey) == 0 then
                 redis.call('DEL', idxKey)
               end
            end
            
            redis.call('DEL', metaKey)
          end
          
          removedCount = removedCount + 1
        end
        
        if redis.call('SCARD', idsKey) == 0 then
          redis.call('DEL', idsKey)
        end
        
        return removedCount
        
        --[[
        local ids = redis.call('SMEMBERS', targetKey)
        if #ids == 0 then
          return 0
        end
        
        -- Cache index names start at ARGV[5]
        local indexNamesStart = 5
        
        local removedCount = 0
        for _, id in ipairs(ids) do
          local valKey = prefix .. ":__val__:" .. id
          redis.call('DEL', valKey)
          redis.call('SREM', idsKey, id)
          
          -- Remove id from all indexes using meta-sets
          local j = indexNamesStart
          for _ = 1, indexCount do
            local idxName = ARGV[j]; j = j + 1
            local metaKey = prefix .. ":__meta__:" .. id .. ":" .. idxName
            local vals = redis.call('SMEMBERS', metaKey)
            for _, v in ipairs(vals) do
              local idxKey = prefix .. ":__idx__:" .. idxName .. ":" .. v
              redis.call('SREM', idxKey, id)
              if redis.call('SCARD', idxKey) == 0 then
                redis.call('DEL', idxKey)
              end
            end
            redis.call('DEL', metaKey)
          end
          
          removedCount = removedCount + 1
        end
        
        redis.call('DEL', targetKey)
        
        if redis.call('SCARD', idsKey) == 0 then
          redis.call('DEL', idsKey)
        end
        
        return removedCount
        --]]
    """.trimIndent()
    ),

    @Language("Lua")
    TOUCH_VALUE(
        """
        -- ARGV:
        -- 1: prefix
        -- 2: id
        -- 3: ttlMillis
        -- 4: indexCount
        -- 5..: indexName1..indexNameN
        --
        -- Returns:
        --   1 if value exists, else 0
    
        local prefix = ARGV[1]
        local id = ARGV[2]
        local ttl = tonumber(ARGV[3])
        local indexCount = tonumber(ARGV[4])
    
        local valKey = prefix .. ":__val__:" .. id
        local idsKey = prefix .. ":__ids__"
    
        if redis.call('EXISTS', valKey) == 0 then
          return 0
        end
    
        -- Keep canonical ids tracking alive (and repair if missing)
        redis.call('SADD', idsKey, id)
        redis.call('PEXPIRE', idsKey, ttl)
    
        -- Touch value
        redis.call('PEXPIRE', valKey, ttl)
    
        -- Touch meta keys
        local i = 5
        for _ = 1, indexCount do
          local idxName = ARGV[i]; i = i + 1
          local metaKey = prefix .. ":__meta__:" .. id .. ":" .. idxName
          redis.call('PEXPIRE', metaKey, ttl)
        end
    
        return 1
    """.trimIndent()
    );


    companion object {
        private val scriptShas = Caffeine.newBuilder()
            .asCache<SimpleSetRedisCacheLuaScripts, String>()


        fun <R> executeReactive(
            script: RScriptReactive,
            mode: RScript.Mode,
            lua: SimpleSetRedisCacheLuaScripts,
            returnType: RScript.ReturnType,
            keys: List<Any>,
            vararg values: Any,
            tries: Int = 3
        ): Mono<R> {
            val sha = mono {
                scriptShas.get(lua) {
                    script.scriptLoad(lua.script).awaitSingleOrNull() ?: error("Failed to load Lua script")
                }
            }

            return sha
                .flatMap { script.evalSha<R>(mode, it, returnType, keys, *values) }
                .doOnError(RedisNoScriptException::class.java) { scriptShas.invalidate(lua) }
                .retryWhen(
                    Retry.max(tries.toLong())
                        .filter { it is RedisNoScriptException }
                )
        }

        suspend fun <R> execute(
            script: RScriptReactive,
            mode: RScript.Mode,
            lua: SimpleSetRedisCacheLuaScripts,
            returnType: RScript.ReturnType,
            keys: List<Any>,
            vararg values: Any,
            tries: Int = 3
        ): R {
           return executeReactive<R>(script, mode, lua, returnType, keys, *values, tries = tries).awaitSingle()
        }
    }
}