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
local eventType  = ARGV[7] -- "ALL"
local prefix     = ARGV[8]

local removed    = 0
while true do
    local id = redis.call('SPOP', idsKey)
    if not id then break end
    redis.call('DEL', prefix .. ":__val__:" .. id)
    removed = removed + 1
end
redis.call('DEL', idsKey)

if removed == 0 then
    return 0
end

local ver = redis.call('INCR', versionKey)
local msg = tostring(ver) .. delim .. originId .. delim
redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, eventType, fieldMsg, msg)

redis.call('PEXPIRE', streamKey, ttl)
redis.call('PEXPIRE', versionKey, ttl)

return removed
