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

local idx        = tonumber(ARGV[7])
local tombstone  = ARGV[8]

-- Validate index
local old        = redis.call('LINDEX', dataKey, idx)
if old == false then
    return 0
end

-- Set tombstone at index, remove it, increment version, add message to stream
redis.call('LSET', dataKey, idx, tombstone)
redis.call('LREM', dataKey, 1, tombstone)
local ver = redis.call('INCR', versionKey)

local payload = tostring(idx) .. delim .. tostring(old)
local msg = tostring(ver) .. delim .. originId .. delim .. payload
redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, eventType, fieldMsg, msg)

return ver