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

local newValue   = ARGV[7]

local old        = redis.call('GET', dataKey)

redis.call('SET', dataKey, newValue, 'KEEPTTL')
local ver = redis.call('INCR', versionKey)

-- Same event as set.lua: payload is the new value
local payload = newValue
local msg = tostring(ver) .. delim .. originId .. delim .. payload
redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, eventType, fieldMsg, msg)

-- Extra results: presence flag and previous value
if old == false then
    return { ver, payload, '0', '' }
end

return { ver, payload, '1', old }
