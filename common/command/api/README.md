# surf-api-command-api

Platform-independent command helpers on top of CommandAPI and Brigadier.

- `SurfCommandUtil` and the command exception builder
- a Brigadier exception that carries a component
- suspending argument helpers and `CommandRegistrar`

It has no runtime half; each platform runtime registers the commands. See the `surf-api-commands`
skill.
