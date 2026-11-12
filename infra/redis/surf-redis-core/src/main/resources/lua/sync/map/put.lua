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
local value      = ARGV[8]

local old        = redis.call('HGET', dataKey, key)

-- If old value is same as new value, return 0 (no-op)
if old ~= false and old == value then
    return 0
end

-- Set new value and increment version
redis.call('HSET', dataKey, key, value)
local ver = redis.call('INCR', versionKey)

-- Create and add message to stream — include old value if present
local payload = key .. delim .. value
if old then
    payload = payload .. delim .. old
end
local msg = tostring(ver) .. delim .. originId .. delim .. payload
redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, eventType, fieldMsg, msg)

return ver
