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
local expected   = ARGV[8]
local tombstone  = ARGV[9]

local current    = redis.call('LINDEX', dataKey, idx)

if current == false or current ~= expected then
    return -1
end

redis.call('LSET', dataKey, idx, tombstone)
redis.call('LREM', dataKey, 1, tombstone)

local version = redis.call('INCR', versionKey)

local payload =
    tostring(idx) .. delim ..
    current

local message =
    tostring(version) .. delim ..
    originId .. delim ..
    payload

redis.call(
    'XADD',
    streamKey,
    'MAXLEN', '~', maxLen,
    '*',
    fieldType, eventType,
    fieldMsg, message
)

return version
