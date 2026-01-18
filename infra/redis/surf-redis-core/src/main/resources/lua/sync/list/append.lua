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

local value      = ARGV[7]

-- Append value to list and get new version and index
redis.call('RPUSH', dataKey, value)
local ver = redis.call('INCR', versionKey)
local idx = redis.call('LLEN', dataKey) - 1

-- Create and add message to stream
local payload = tostring(idx) .. delim .. value
local msg = tostring(ver) .. delim .. originId .. delim .. payload
redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, eventType, fieldMsg, msg)

return ver