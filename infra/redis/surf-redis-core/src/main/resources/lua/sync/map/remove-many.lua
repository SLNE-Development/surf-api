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

for i = 7, #ARGV do
    local key = ARGV[i]

    local old = redis.call('HGET', dataKey, key)
    if old ~= false then
        -- Remove key
        redis.call('HDEL', dataKey, key)

        -- Increment version
        local ver = redis.call('INCR', versionKey)

        -- Create and add message to stream
        local payload = key .. delim .. old
        local msg = tostring(ver) .. delim .. originId .. delim .. payload
        redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, eventType, fieldMsg, msg)
    end
end

return 0