--[[
ARGV:
1) prefix      (cache key prefix)
2) id          (ID of the element)
3) json        (serialized value)
4) ttlMillis   (TTL in milliseconds)
5) indexCount  (number of indexes configured)
6..) For each index:
    - indexName
    - valueCount
    - value1 .. valueN  (the values for this index in the new element)

Operation:
- Set/update the value with TTL.
- Add ID to the IDs set (track existence) and update its TTL.
- For each index:
    * Compute the new set of values vs. old set (from existing meta).
    * Remove ID from index sets that are no longer applicable.
    * Add ID to index sets for new values.
    * Update the meta set for this ID and index name to the new values.
- Each time an index entry is added or removed, record that indexName/indexValue as "touched".
- Return [wasNew, ...touchedEntries].
- Stream events:
    * Always: VAL event for this ID.
    * If wasNew: IDS event.
    * For each touched index entry: IDX event (with indexName and indexValue).
]]
local prefix = ARGV[1]
local id = ARGV[2]
local json = ARGV[3]
local ttl = tonumber(ARGV[4])
local indexCount = tonumber(ARGV[5])
local sep = string.char(0)

local idsKey = prefix .. ":__ids__"
local valKey = prefix .. ":__val__:" .. id
local streamKey = prefix .. ":__stream__"
local verKey = prefix .. ":__version__"

-- Upsert the value with TTL
redis.call('SET', valKey, json, 'PX', ttl)
-- Add ID to global IDs set
local wasNew = redis.call('SADD', idsKey, id)
redis.call('PEXPIRE', idsKey, ttl)

local touched = {}
local argIndex = 6
for i = 1, indexCount do
    local idxName = ARGV[argIndex]; argIndex = argIndex + 1
    local newCount = tonumber(ARGV[argIndex]); argIndex = argIndex + 1

    local metaKey = prefix .. ":__meta__:" .. id .. ":" .. idxName
    -- Get old index values from meta set
    local oldVals = redis.call('SMEMBERS', metaKey)
    local oldSet = {}
    for _, v in ipairs(oldVals) do
        oldSet[v] = true
    end

    local newSet = {}
    local newVals = {}
    for j = 1, newCount do
        local v = ARGV[argIndex]; argIndex = argIndex + 1
        if not newSet[v] then
            newSet[v] = true
            table.insert(newVals, v)
        end
    end

    -- Remove ID from index entries that are no longer present
    for _, v in ipairs(oldVals) do
        if not newSet[v] then
            local idxKey = prefix .. ":__idx__:" .. idxName .. ":" .. v
            redis.call('SREM', idxKey, id)
            if redis.call('SCARD', idxKey) == 0 then
                redis.call('DEL', idxKey)  -- remove empty index set
            else
                redis.call('PEXPIRE', idxKey, ttl)
            end
            table.insert(touched, idxName .. sep .. v)
        end
    end

    -- Add ID to new index entries
    for _, v in ipairs(newVals) do
        local idxKey = prefix .. ":__idx__:" .. idxName .. ":" .. v
        local added = redis.call('SADD', idxKey, id)
        if added == 1 then
            table.insert(touched, idxName .. sep .. v)
        end
        redis.call('PEXPIRE', idxKey, ttl)
    end

    -- Update meta set for this id-index
    redis.call('DEL', metaKey)
    if #newVals > 0 then
        redis.call('SADD', metaKey, unpack(newVals))
    end
    redis.call('PEXPIRE', metaKey, ttl)
end

-- Prepare events: determine how many events to send
local eventCount = 1 + (wasNew == 1 and 1 or 0) + #touched  -- VAL + optional IDS + one per touched index
if eventCount > 0 then
    local newVersion = redis.call('INCRBY', verKey, eventCount)
    local firstVer = newVersion - eventCount + 1
    local curVer = firstVer

    -- Always emit VAL event for this id
    local msgVal = tostring(curVer) .. sep .. prefix  -- use prefix as origin? Actually use origin outside script, but not passed here.
    -- We actually need origin here; since origin not passed to script, use an empty placeholder or prefix.
    -- For simplicity, using prefix as origin in this context.
    msgVal = tostring(curVer) .. sep .. prefix .. sep .. id
    redis.call('XADD', streamKey, 'MAXLEN', '~', '10000', '*', 'T', 'VAL', 'M', msgVal)
    curVer = curVer + 1

    -- If new ID, emit IDS event
    if wasNew == 1 then
        local msgIds = tostring(curVer) .. sep .. prefix
        redis.call('XADD', streamKey, 'MAXLEN', '~', '10000', '*', 'T', 'IDS', 'M', msgIds)
        curVer = curVer + 1
    end

    -- For each touched index entry, emit an IDX event
    for _, entry in ipairs(touched) do
        local msgIdx = tostring(curVer) .. sep .. prefix .. sep .. entry
        redis.call('XADD', streamKey, 'MAXLEN', '~', '10000', '*', 'T', 'IDX', 'M', msgIdx)
        curVer = curVer + 1
    end

    -- Set TTL for stream and version keys
    redis.call('PEXPIRE', verKey, ttl)
    redis.call('PEXPIRE', streamKey, ttl)
end

-- Prepare return result
local result = {}
result[1] = wasNew
for i, entry in ipairs(touched) do
    result[i+1] = entry
end
return result
