local prefix = ARGV[1]
local id = ARGV[2]
local ttl = tonumber(ARGV[3])
local origin = ARGV[4]
local sep = string.char(0)

local idsKey = prefix .. ":__ids__"
local valKey = prefix .. ":__val__:" .. id
local streamKey = prefix .. ":__stream__"
local verKey = prefix .. ":__version__"
local NULL_MARKER = "__NULL__"

-- Set the null marker with TTL
redis.call('SET', valKey, NULL_MARKER, 'PX', ttl)

-- Ensure the ID is tracked in the IDs set and update TTL
redis.call('SADD', idsKey, id)
redis.call('PEXPIRE', idsKey, ttl)

-- Publish stream event for this key
local newVersion = redis.call('INCR', verKey)
local message = tostring(newVersion) .. sep .. origin .. sep .. id
redis.call('XADD', streamKey, 'MAXLEN', '~', '10000', '*', 'T', 'VAL', 'M', message)
redis.call('PEXPIRE', verKey, ttl)
redis.call('PEXPIRE', streamKey, ttl)
