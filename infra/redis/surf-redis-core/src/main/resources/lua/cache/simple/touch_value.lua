-- KEYS
local idsKey = KEYS[1]

-- ARGV
local ttl    = tonumber(ARGV[1])
local id     = ARGV[2]
local prefix = ARGV[3]

local valKey = prefix .. ":__val__:" .. id

if redis.call('EXISTS', valKey) == 0 then
  return 0
end

redis.call('SADD', idsKey, id)
redis.call('PEXPIRE', idsKey, ttl)
redis.call('PEXPIRE', valKey, ttl)

return 1
