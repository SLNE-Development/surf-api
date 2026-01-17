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

local element    = ARGV[7]

-- Try to remove element from set
local removed    = redis.call('SREM', dataKey, element)

-- If removed, increment version, add message to stream
if removed == 1 then
    local ver = redis.call('INCR', versionKey)

    local payload = element
    local msg = tostring(ver) .. delim .. originId .. delim .. payload
    redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, eventType, fieldMsg, msg)

    return ver
end

-- Element was not present (dirty client), return -1
return -1
