# surf-api-paper-scenario-owner

A Paper plugin without code that the bot-driven scenario tests name as the owner of temporary
inventory sessions that must outlive their owner. A test disables it while the server runs, or
removes its jar to restart the server without the plugin that began a session. The probe plugin of
`local/paper-scenario-probe` stays enabled and keeps observing.

Local only. See `local/paper-scenario-test`.
