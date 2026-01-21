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
local eventType  = ARGV[7] -- "VAL"
local id         = ARGV[8]
local prefix     = ARGV[9]

local valKey     = prefix .. ":__val__:" .. id

local removedVal = redis.call('DEL', valKey)
local removedId  = redis.call('SREM', idsKey, id)

if redis.call('SCARD', idsKey) == 0 then
    redis.call('DEL', idsKey)
end

if (removedVal + removedId) == 0 then
    return 0
end

local ver = redis.call('INCR', versionKey)
local msg = tostring(ver) .. delim .. originId .. delim .. id
redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, eventType, fieldMsg, msg)

redis.call('PEXPIRE', streamKey, ttl)
redis.call('PEXPIRE', versionKey, ttl)

return 1
