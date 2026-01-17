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


-- Remove each value from list, creating message for each removal
local removedCount = 0
for i = 7, #ARGV do
    local toRemove = ARGV[i]
    local removed  = redis.call('LREM', dataKey, 1, toRemove)

    if removed == 1 then
        removedCount = removedCount + 1

        local ver = redis.call('INCR', versionKey)
        local payload = tostring(toRemove)
        local msg = tostring(ver) .. delim .. originId .. delim .. payload
        redis.call('XADD', streamKey, 'MAXLEN', '~', maxLen, '*', fieldType, eventType, fieldMsg, msg)
    end
end

return 0