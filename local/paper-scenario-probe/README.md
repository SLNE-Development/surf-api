# surf-api-paper-scenario-probe

The Paper plugin that the bot-driven scenario tests install. It answers console requests about
inventories, blocks and dropped items, and logs what players do.

- `ProbeProtocol`: the console command and the log lines the tests read
- `ProbeAction`: one request; a new bean that implements it adds a request
- `ProbeEventLog`: logs joins, quits, deaths, respawns and opened and closed inventories

Local only. See `local/paper-scenario-test`.
