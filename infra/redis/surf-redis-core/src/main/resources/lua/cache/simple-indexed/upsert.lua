-- KEYS
local idsKey     = KEYS[1]
local streamKey  = KEYS[2]
local versionKey = KEYS[3]

-- ARGV
local originId   = ARGV[1]
local delim      = ARGV[2]
local maxLen     = tonumber(ARGV[3])
local ttl        = tonumber(ARGV[4])
local fieldType  = ARGV[5]
local fieldMsg   = ARGV[6]

-- ARGV layout after header:
-- 7: prefix
-- 8: id
-- 9: json
-- 10: indexCount
-- then per index: name, valueCount, values...
local prefix     = ARGV[7]
local id         = ARGV[8]
local json       = ARGV[9]
local indexCount = tonumber(ARGV[10])

local sep = delim

local valKey = prefix .. ":__val__:" .. id
redis.call('SET', valKey, json, 'PX', ttl)

local wasNew = redis.call('SADD', idsKey, id)
redis.call('PEXPIRE', idsKey, ttl)

local touched = {}
local i = 11

for _ = 1, indexCount do
  local idxName = ARGV[i]; i = i + 1
  local newCount = tonumber(ARGV[i]); i = i + 1

  local metaKey = prefix .. ":__meta__:" .. id .. ":" .. idxName

  local oldVals = redis.call('SMEMBERS', metaKey)
  local oldMap = {}
  for _, v in ipairs(oldVals) do oldMap[v] = true end

  local newMap = {}
  local newVals = {}
  for _ = 1, newCount do
    local v = ARGV[i]; i = i + 1
    if not newMap[v] then
      newMap[v] = true
      newVals[#newVals + 1] = v
    end
  end

  -- removals
  for _, v in ipairs(oldVals) do
    if not newMap[v] then
      local idxKey = prefix .. ":__idx__:" .. idxName .. ":" .. v
      redis.call('SREM', idxKey, id)
      if redis.call('SCARD', idxKey) == 0 then
        redis.call('DEL', idxKey)
      else
        redis.call('PEXPIRE', idxKey, ttl)
      end
      touched[#touched + 1] = idxName .. sep .. v
    end
  end

  -- additions
  for _, v in ipairs(newVals) do
    local idxKey = prefix .. ":__idx__:" .. idxName .. ":" .. v
    local added = redis.call('SADD', idxKey, id)
    if added == 1 then
      touched[#touched + 1] = idxName .. sep .. v
    end
    redis.call('PEXPIRE', idxKey, ttl)
  end

  redis.call('DEL', metaKey)
  if #newVals > 0 then
    redis.call('SADD', metaKey, unpack(newVals))
  end
  redis.call('PEXPIRE', metaKey, ttl)
end

-- Events: VAL always, IDS if wasNew, IDX for each touched
local eventCount = 1 + (wasNew == 1 and 1 or 0) + #touched
local lastVer = redis.call('INCRBY', versionKey, eventCount)
local ver = lastVer - eventCount + 1

-- VAL
local msgVal = tostring(ver) .. sep .. originId .. sep .. id
redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, 'V', fieldMsg, msgVal)
ver = ver + 1

-- IDS
if wasNew == 1 then
  local msgIds = tostring(ver) .. sep .. originId .. sep
  redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, 'IS', fieldMsg, msgIds)
  ver = ver + 1
end

-- IDX
for _, entry in ipairs(touched) do
  local msgIdx = tostring(ver) .. sep .. originId .. sep .. entry
  redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, 'IX', fieldMsg, msgIdx)
  ver = ver + 1
end

redis.call('PEXPIRE', streamKey, ttl)
redis.call('PEXPIRE', versionKey, ttl)

local res = { wasNew }
for _, t in ipairs(touched) do
  res[#res + 1] = t
end
return res
