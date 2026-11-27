# surf-api-paper-scenario-owner

A Paper plugin without code that the bot-driven scenario tests name as the owner of a temporary
inventory session. A test removes its jar to restart the server without the plugin that began the
session, while the probe plugin of `local/paper-scenario-probe` stays installed and keeps
observing.

Local only. See `local/paper-scenario-test`.
