-- Keys
local dataKey      = KEYS[1]
local streamKey    = KEYS[2]
local versionKey   = KEYS[3]

-- Args
local originId     = ARGV[1]
local delim        = ARGV[2]
local maxLen       = tonumber(ARGV[3])
local fieldType    = ARGV[4]
local fieldMsg     = ARGV[5]
local eventType    = ARGV[6]

local removedCount = 0
for i = 7, #ARGV do
    local element = ARGV[i]

    -- Try to remove element from set
    local removed = redis.call('SREM', dataKey, element)

    -- If removed, increment removed count
    if removed == 1 then
        removedCount = removedCount + 1

        -- Increment version, add message to stream
        local ver = redis.call('INCR', versionKey)

        local payload = element
        local msg = tostring(ver) .. delim .. originId .. delim .. payload
        redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, eventType, fieldMsg, msg)
    end
end

return 0
