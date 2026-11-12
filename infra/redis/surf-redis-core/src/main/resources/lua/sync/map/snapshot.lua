local dataKey    = KEYS[1]
local versionKey = KEYS[2]

local result     = redis.call('HGETALL', dataKey)
local version    = redis.call('GET', versionKey) or '0'

table.insert(result, tostring(version))

return result
