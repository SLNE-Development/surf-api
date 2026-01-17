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

-- Check if list exists, delete it
local existed    = redis.call('EXISTS', dataKey)
redis.call('DEL', dataKey)

-- If list existed, increment version and add clear message to stream
if existed == 1 then
    local ver = redis.call('INCR', versionKey)
    local payload = "" -- No additional payload for clear
    local msg = tostring(ver) .. delim .. originId .. delim .. payload
    redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, eventType, fieldMsg, msg)
    return ver
end

-- If list did not exist, return -1 (dirty origin)
return -1
