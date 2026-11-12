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

-- 7: prefix, 8: targetIndexName, 9: targetIndexValue, 10: indexCount, 11..: indexNames
local prefix     = ARGV[7]
local targetName = ARGV[8]
local targetVal  = ARGV[9]
local indexCount = tonumber(ARGV[10])

local sep = delim

local targetKey = prefix .. ":__idx__:" .. targetName .. ":" .. targetVal
local removedCount = 0

while true do
  local id = redis.call('SPOP', targetKey)
  if not id then break end

  redis.call('DEL', prefix .. ":__val__:" .. id)
  redis.call('SREM', idsKey, id)

  local j = 11
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

if removedCount == 0 then
  return 0
end

local ver = redis.call('INCR', versionKey)
local msg = tostring(ver) .. sep .. originId .. sep
redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, 'A', fieldMsg, msg)

redis.call('PEXPIRE', streamKey, ttl)
redis.call('PEXPIRE', versionKey, ttl)

return removedCount
