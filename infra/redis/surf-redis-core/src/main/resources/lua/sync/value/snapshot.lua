local dataKey    = KEYS[1]
local versionKey = KEYS[2]

local value      = redis.call('GET', dataKey)
local version    = redis.call('GET', versionKey) or '0'

if value == false then
    return { '0', '', tostring(version) }
end

return { '1', value, tostring(version) }
