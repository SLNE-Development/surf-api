# surf-api-common

This module contains common data structures and models that are shared between multiple surf-api modules, particularly between the annotation processor (surf-api-processor) and the runtime modules (surf-api-core).

## Purpose

The primary purpose of this module is to provide a single source of truth for data structures that are used for serialization contracts between compile-time processing and runtime execution. This prevents schema drift and runtime decode failures that could occur when maintaining duplicate copies of the same data structures in different modules.

## Current Contents

### Hook Metadata (`dev.slne.surf.surfapi.common.hook`)

- **PluginHookMeta**: Data class representing the metadata for plugin hooks, serialized to `surf-hooks.json` by the annotation processor and read at runtime by the hook service.

## Usage

This module is automatically included as a dependency in:
- `surf-api-processor` (for generating metadata during compilation)
- `surf-api-core-server` (for reading metadata at runtime)

## Adding New Common Structures

When you need to add a new data structure that must be shared between the processor and runtime:

1. Add the class to an appropriate package under `dev.slne.surf.surfapi.common`
2. Annotate it with `@Serializable` if it needs to be serialized/deserialized
3. Update the modules that need to use it to depend on `surf-api-common`
