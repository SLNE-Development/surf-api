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
local newValue   = ARGV[8]

local current    = redis.call('LINDEX', dataKey, idx)

-- Index out of range: nothing changes
if current == false then
    return { 0 }
end

redis.call('LSET', dataKey, idx, newValue)
local ver = redis.call('INCR', versionKey)

-- Same event as set-at.lua: index, previous value and new value
local payload = tostring(idx) .. delim .. current .. delim .. newValue
local msg = tostring(ver) .. delim .. originId .. delim .. payload
redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, eventType, fieldMsg, msg)

return { ver, payload }
