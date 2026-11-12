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

local existing   = redis.call('HGET', dataKey, key)

-- Key present: nothing changes, report the existing value as extra result
if existing ~= false then
    return { 0, '', existing }
end

redis.call('HSET', dataKey, key, value)
local ver = redis.call('INCR', versionKey)

-- Same event as put.lua for a previously absent key: key and value only
local payload = key .. delim .. value
local msg = tostring(ver) .. delim .. originId .. delim .. payload
redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, eventType, fieldMsg, msg)

return { ver, payload }
