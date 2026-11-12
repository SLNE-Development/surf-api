local dataKey    = KEYS[1]
local streamKey  = KEYS[2]
local versionKey = KEYS[3]

local originId   = ARGV[1]
local delim      = ARGV[2]
local maxLen     = tonumber(ARGV[3])
local fieldType  = ARGV[4]
local fieldMsg   = ARGV[5]
local eventType  = ARGV[6]

local key        = ARGV[7]
local expected   = ARGV[8]
local newValue   = ARGV[9]

local current    = redis.call('HGET', dataKey, key)

if current == false or current ~= expected then
    return 0
end

redis.call('HSET', dataKey, key, newValue)

local version = redis.call('INCR', versionKey)

local payload =
    key .. delim ..
    newValue .. delim ..
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
