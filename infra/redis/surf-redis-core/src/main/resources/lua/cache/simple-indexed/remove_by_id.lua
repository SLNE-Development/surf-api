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

-- 7: prefix, 8: id, 9: indexCount, 10..: indexNames
local prefix     = ARGV[7]
local id         = ARGV[8]
local indexCount = tonumber(ARGV[9])

local sep = delim

local valKey = prefix .. ":__val__:" .. id
local removedVal = redis.call('DEL', valKey)
local removedId  = redis.call('SREM', idsKey, id)

if (removedVal + removedId) == 0 then
  return {0}
end

local touched = {}
local i = 10
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

local eventCount = 1 + 1 + #touched  -- VAL + IDS + IDX*
local lastVer = redis.call('INCRBY', versionKey, eventCount)
local ver = lastVer - eventCount + 1

-- VAL
local msgVal = tostring(ver) .. sep .. originId .. sep .. id
redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, 'V', fieldMsg, msgVal)
ver = ver + 1

-- IDS
local msgIds = tostring(ver) .. sep .. originId .. sep
redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, 'IS', fieldMsg, msgIds)
ver = ver + 1

-- IDX
for _, entry in ipairs(touched) do
  local msgIdx = tostring(ver) .. sep .. originId .. sep .. entry
  redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, 'IX', fieldMsg, msgIdx)
  ver = ver + 1
end

redis.call('PEXPIRE', streamKey, ttl)
redis.call('PEXPIRE', versionKey, ttl)

local res = {1}
for _, t in ipairs(touched) do
  res[#res + 1] = t
end
return res
