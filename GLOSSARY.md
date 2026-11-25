# surf-api

surf-api is the shared library behind SLNE's Paper, Velocity and Minestom plugins and standalone services. This glossary fixes the words for concepts that are specific to it.

## Message roles

The roles a piece of chat text plays. Each role has one color, so players read the same meaning from the same color everywhere.

**Prefix**:
The marker that opens a system message and names its kind: info, success, warning or error.

**Primary**:
The main text of a message.

**Secondary**:
Supporting text that is less important than the primary text.

**Spacer**:
Punctuation and separators between the parts of a message.
_Avoid_: separator color, delimiter

**Note**:
An aside that adds context without being part of the message's statement.

**Variable key**:
The name of a value shown to the player, such as "Coins".

**Variable value**:
A value shown to the player, such as a player name or an amount.

**Info / Success / Warning / Error**:
The four kinds of system message, each with its own prefix and color.

## Inventories

**Item insertion**:
Putting item stacks into an inventory, stacking onto similar items before using empty slots.
_Avoid_: transaction, transfer, delivery

**Insertion plan**:
A preview of an item insertion that tells how much fits and how much would be left over, without changing the inventory.

**Item removal**:
Taking a number of similar items out of an inventory.
_Avoid_: transaction, withdrawal

**Removal plan**:
A preview of an item removal that tells whether the inventory holds enough, without changing it.

**Insertion policy**:
What committing an insertion plan does when the inventory changed and the items no longer fit: insert nothing, insert what fits, or drop the rest.
_Avoid_: overflow policy

**Removal policy**:
What committing a removal plan does when the inventory no longer holds enough: remove nothing, or remove what is there.

**Storage slots**:
The hotbar and main inventory of a player, without armor, offhand and crafting grid.
