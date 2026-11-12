-- Keys
local dataKey    = KEYS[1]
local streamKey  = KEYS[2]
local versionKey = KEYS[3]

-- Args
local originId   = ARGV[1]
local delim      = ARGV[2]
local maxLen     = tonumber(ARGV[3])
local fieldType  = ARGV[4]
local fieldMsg   = ARGV[5]
local eventType  = ARGV[6]

local key        = ARGV[7]

local old        = redis.call('HGET', dataKey, key)
if old == false then
    return { 0 }
end

redis.call('HDEL', dataKey, key)
local ver = redis.call('INCR', versionKey)

-- Same event as remove.lua: key and the removed value
local payload = key .. delim .. old
local msg = tostring(ver) .. delim .. originId .. delim .. payload
redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, eventType, fieldMsg, msg)

return { ver, payload }
