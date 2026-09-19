-- Keys
local dataKey     = KEYS[1]
local streamKey   = KEYS[2]
local versionKey  = KEYS[3]

-- Args
local originId    = ARGV[1]
local delim       = ARGV[2]
local maxLen      = tonumber(ARGV[3])
local fieldType   = ARGV[4]
local fieldMsg    = ARGV[5]
local eventType   = ARGV[6]

local expected    = ARGV[7]
local newValue    = ARGV[8]
-- Value an absent key is compared as
local absentValue = ARGV[9]

local current     = redis.call('GET', dataKey) or absentValue

if current ~= expected then
    return { 0 }
end

redis.call('SET', dataKey, newValue, 'KEEPTTL')
local ver = redis.call('INCR', versionKey)

-- Same event as set.lua: payload is the new value
local payload = newValue
local msg = tostring(ver) .. delim .. originId .. delim .. payload
redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, eventType, fieldMsg, msg)

return { ver, payload }
