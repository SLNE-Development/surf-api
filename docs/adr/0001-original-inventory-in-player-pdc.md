# The original inventory lives only in the player's persistent data

A temporary inventory session stores the original inventory in the player's
PersistentDataContainer and nowhere else. The server writes inventory, equipment and that container
into the same playerdata file in one save, so after a crash, a `.dat_old` fallback or a restored
backup the session always matches the inventory it describes. A database or a separate file has its
own save timeline: a crash then either restores an original that is newer than the rolled-back game
state, or loses the original after the session ended. Keeping a marker in the container and the
items elsewhere still loses the original in the second case.

## Consequences

- Recovery happens only when a server with surf-api loads the player again; until then the
  temporary inventory stays in the offline playerdata.
- A plugin that clears the player's container also deletes the original inventory.
- Every save of a player in a session also carries the original inventory.
