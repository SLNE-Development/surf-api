local prefix = ARGV[1]
local id = ARGV[2]
local origin = ARGV[3]
local sep = string.char(0)

local idsKey = prefix .. ":__ids__"
local valKey = prefix .. ":__val__:" .. id
local streamKey = prefix .. ":__stream__"
local verKey = prefix .. ":__version__"

-- Delete value key and remove from IDs set
local removedVal = redis.call('DEL', valKey)
local removedId = redis.call('SREM', idsKey, id)

local removed = 0
if (removedVal + removedId) > 0 then
    removed = 1
    -- If something was removed, publish an invalidation event for this key
    local newVersion = redis.call('INCR', verKey)
    local message = tostring(newVersion) .. sep .. origin .. sep .. id
    redis.call('XADD', streamKey, 'MAXLEN', '~', '10000', '*', 'T', 'VAL', 'M', message)
end

-- If IDs set is empty after removal, delete it (no keys left in cache)
if redis.call('SCARD', idsKey) == 0 then
    redis.call('DEL', idsKey)
end

return removed
