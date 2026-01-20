local prefix = ARGV[1]
local id = ARGV[2]
local json = ARGV[3]
local ttl = tonumber(ARGV[4])
local origin = ARGV[5]
local sep = string.char(0)

local idsKey = prefix .. ":__ids__"
local valKey = prefix .. ":__val__:" .. id
local streamKey = prefix .. ":__stream__"
local verKey = prefix .. ":__version__"

-- Set the value with TTL
redis.call('SET', valKey, json, 'PX', ttl)

-- Add id to IDs set (to keep track of all keys) and update TTL
redis.call('SADD', idsKey, id)
redis.call('PEXPIRE', idsKey, ttl)

-- Generate stream event
local newVersion = redis.call('INCR', verKey)
local message = tostring(newVersion) .. sep .. origin .. sep .. id
-- Use MAXLEN ~ 10000 to cap stream length
redis.call('XADD', streamKey, 'MAXLEN', '~', '10000', '*', 'T', 'VAL', 'M', message)
-- Set expiration on version and stream keys to align with data TTL
redis.call('PEXPIRE', verKey, ttl)
redis.call('PEXPIRE', streamKey, ttl)
