# External callers

This guide is for applications that do not run surf-nats on the JVM but exchange messages with applications that
do, for example a web panel written in TypeScript. There are two ways to connect:

- On Node.js, use the client `@slne/surf-nats` with [protobuf-es](https://github.com/bufbuild/protobuf-es) schemas
  (see [TypeScript client](#typescript-client)). It implements everything in this guide.
- With any other NATS client, follow the [wire format](#wire-format) below and encode the messages with a Protocol
  Buffers library.

Read the [README](../README.md) first for the concepts: clients, contracts, events, services, and persistent
methods.

## Before you start

- **Get the contracts.** The jar of a contract module holds its `.proto` files under `META-INF/surf-nats/proto/`.
  The contracts import `surf/nats/v1/options.proto` and the `surf/types/v1` messages from the jar of
  `surf-nats-types-common`, and Paper contracts the `surf/types/paper/v1` messages from `surf-nats-types-paper`.
  Copy all of them into one directory. The `google/protobuf` files come with your Protocol Buffers tooling.
- **Know the owner.** The subjects of an event or service that is not `global` contain the client name of the
  application that owns it, for example `surf-punish`. Your application needs that name. Global contracts have no
  owner.
- **Prefer explicit names.** Contracts that external applications use should set `subject` on events and `name` on
  services and methods. Derived names change when someone renames a message or an `rpc`.
- **Register persistent services first.** Registration on the JVM creates the JetStream stream of a persistent
  service. External callers never create it, and their persistent calls fail until the service has been registered
  once.

How you call a method depends on whether it is persistent, which instances it addresses, and what it returns (see
[What a call does](../README.md#what-a-call-does)). The contract shows all three: look for `persistent` and
`recipients` in the `(surf.nats.v1.service)` and `(surf.nats.v1.method)` options, and for a response of type
`google.protobuf.Empty`.

The examples in this guide use the global event `NameChanged` from the README and this global service:

```proto
// surf/roles/v1/roles_service.proto
syntax = "proto2";

package surf.roles.v1;

option java_package = "dev.slne.surf.roles.proto.v1";

import "google/protobuf/empty.proto";
import "surf/nats/v1/options.proto";

service RolesService {
  option (surf.nats.v1.service) = { name: "roles" global: true };

  rpc Lookup(LookupRequest) returns (LookupResponse) {
    option (surf.nats.v1.method) = { name: "lookup" };
  }
  rpc Whitelist(WhitelistRequest) returns (google.protobuf.Empty) {
    option (surf.nats.v1.method) = { name: "whitelist" persistent: true };
  }
}

message LookupRequest {
  optional int64 user_id = 1;
}

message LookupResponse {
  optional string role = 1 [(surf.nats.v1.nullable) = true];
}

message WhitelistRequest {
  optional WhitelistAction action = 1;
  optional int64 user_id = 2;
}

enum WhitelistAction {
  WHITELIST_ACTION_UNSPECIFIED = 0;
  WHITELIST_ACTION_ADD = 1;
  WHITELIST_ACTION_REMOVE = 2;
}
```

## TypeScript client

`./gradlew :surf-nats-core:jsNodeProductionLibraryDistribution` builds the npm package `@slne/surf-nats` into
`surf-nats-core/build/dist/js/productionLibrary`. How the package will be distributed is not decided yet. It runs
on Node.js, depends on `@nats-io/transport-node`, `@nats-io/jetstream`, and `@nats-io/obj` 3.4.0, and needs
`@bufbuild/protobuf` 2.16.0 as a peer dependency.

### Schemas

Generate the schemas from the contracts with [buf](https://buf.build/) and `@bufbuild/protoc-gen-es` 2.16.0. With
the contracts in `proto/`:

```yaml
# buf.yaml
version: v2
modules:
  - path: proto
```

```yaml
# buf.gen.yaml
version: v2
plugins:
  - local: protoc-gen-es
    out: src/gen
    opt:
      - target=js+dts
      - import_extension=js
```

```sh
npx buf generate
```

Generate `surf/nats/v1/options.proto` together with the contracts: the client reads the surf-nats options, such as
the subject, the service name, and `persistent`, from the generated schemas. protoc-gen-es drops the enum prefix as
Kotlin does, so `WHITELIST_ACTION_ADD` is `WhitelistAction.ADD` in both.

### Connecting

```ts
import { connect } from "@slne/surf-nats";

const nats = await connect({ name: "panel", servers: "nats://localhost:4222", token: "change-me" });

// On shutdown:
await nats.close();
```

`name` is your own client name, in lowercase letters and digits separated by hyphens. It identifies your
application in the `Surf-Origin` header and names the connection. `servers` takes an array or a comma-separated
string and defaults to `nats://localhost:4222`. Set at most one way to authenticate: `token`, `user` with `pass`, or
`credsFile`. The client does not read environment variables; pass their values yourself.

For contracts that are not global, name the owner, either per call with `{ owner: "surf-punish" }` or once with
`nats.target("surf-punish")`, which returns the same operations bound to that owner. Global contracts ignore the
owner.

### Events

```ts
import { NameChangedSchema } from "./gen/surf/profiles/v1/name_changed_pb.js";

await nats.publish(NameChangedSchema, { playerId: "alice", name: "Alice" });

const registration = await nats.listen(NameChangedSchema, (event, context) => {
    console.log(`${event.playerId} is now ${event.name}, published by ${context.origin}`);
});

// To stop listening:
registration.close();
```

`publish` checks the message before it sends it: a field without the `nullable` option and without a `[default]`
must be set, also in nested messages. `publish` returns once it has flushed the connection, or rejects after
`timeoutMs` (default 5000). Delivery is not guaranteed: instances that are offline miss the event.

`listen` delivers every event to this connection by default (`recipients: "ALL_INSTANCES"`). With
`recipients: "ANY_INSTANCE"`, each event goes to one listener among the connections with your client name.
`ignoreFromThisClient: true` skips the events this client published; it works only with `ALL_INSTANCES`. A handler
may return a promise. If it throws or the promise rejects, the client logs the error. Events that cannot be decoded,
or that lack a required field, are dropped with a warning.

### Calls

```ts
import { isFieldSet } from "@bufbuild/protobuf";
import { LookupResponseSchema, RolesService, WhitelistAction } from "./gen/surf/roles/v1/roles_service_pb.js";

const response = await nats.call(RolesService.method.lookup, { userId: 42n });
if (isFieldSet(response, LookupResponseSchema.field.role)) {
    console.log(`user 42 has role ${response.role}`);
}

await nats.call(RolesService.method.whitelist, { action: WhitelistAction.ADD, userId: 42n });
```

`call` returns the response message, or nothing for a response of type `google.protobuf.Empty`. It handles every
kind of method except `ALL_INSTANCES` methods with a response message. Call those with `callAnswer`, which returns
`{ known: true, value }` for the first known answer. Without a known answer, it returns `{ known: false }`, or
throws the first error an instance replied with.
`timeoutMs` overrides the timeout of the contract (default 5 seconds, persistent 60 seconds). Like `publish`, `call`
checks the request before it sends it. Fields with the `nullable` option have explicit presence; check them with
`isFieldSet`.

A persistent call without a result returns once JetStream has stored the call. The client sends it again with the
same `Nats-Msg-Id` until JetStream confirms it or the timeout expires; the timeout can be at most two minutes. The
client creates the object store buckets for [large payloads](#large-payloads) when they are missing, but never a
stream.

Calls reject with these errors:

| Error                   | Cause                                                                                              |
|-------------------------|----------------------------------------------------------------------------------------------------|
| `NatsRemoteError`       | The implementation threw. `type` is the exception class, `remoteMessage` its message.              |
| `NatsNoRespondersError` | No instance serves the method. Only for non-persistent `ANY_INSTANCE` calls. `subject` names it.   |
| `NatsCallTimeoutError`  | No reply, storage confirmation, or flush within `timeoutMs`. Carries `subject` and `timeoutMs`.   |
| `NatsRpcError`          | The base class of all three. Thrown directly when JetStream rejects a persistent call, for example because its stream does not exist. |

A request that lacks a required field, or a non-global contract without an owner, fails before anything is sent.

### UUIDs

A `surf.types.v1.Uuid` holds 16 bytes. `uuidFromString("0b6f1c2e-6d4f-4d1a-9f0e-2c3b1a7e5d90")` returns such a
message, and `uuidToString(uuid)` formats a message or its 16 bytes as a lowercase UUID string. Both throw a
`RangeError` for input of the wrong form.

## Wire format

This section describes the messages for NATS clients other than `@slne/surf-nats`.

### Message format

Every message carries the header `Content-Type: application/protobuf`. A message without it, or with another value,
fails; what happens then depends on the kind of message (see [Errors](#errors)). Header names are case-sensitive:
`content-type` is not `Content-Type`.

The body is the binary Protocol Buffers encoding of a message from the contract: the event message, the request
message of the `rpc`, or its response message. There is no envelope and no type name, because the subject
identifies the message type. An empty body is a valid message with no fields set. It is also the encoding of
`google.protobuf.Empty`.

surf-nats reads messages with these rules:

- A field without the `nullable` option and without a `[default]` is required. Set it even when its value is zero
  or empty, because a message that lacks it fails to decode. Leave a `nullable` field unset to send `null`.
- Unknown fields are ignored, so a newer sender can add fields. Kotlin does not keep them when it encodes a message
  again.
- An enum number that the reader does not know becomes the zero value, also in lists and maps. Other proto2
  implementations leave such a field unset or drop the list element or map entry instead.
- Kotlin reads `uint32` and `uint64` as signed `Int` and `Long`. The wire value stays the same.

The shared types have these fields, all of them required except `Location.world`:

| Type                            | Fields                                                                                 |
|---------------------------------|----------------------------------------------------------------------------------------|
| `surf.types.v1.Uuid`            | `value`: 16 bytes, the most significant 8 bytes first (big-endian)                     |
| `surf.types.v1.Component`       | `json`: the JSON that adventure's Gson serializer writes, a string or an object        |
| `surf.types.v1.Key`             | `value`: the key as `namespace:value`                                                  |
| `surf.types.paper.v1.Location`  | `world` (a `Uuid`, `nullable`), `x`, `y`, `z`, `yaw`, `pitch`                          |
| `surf.types.paper.v1.ItemStack` | `data`: the bytes of Paper's `ItemStack.serializeAsBytes()`                            |

### Subjects

NATS delivers each message by its subject. surf-nats builds subjects as follows:

| What                      | Global                             | Owned by client `profiles`                   |
|---------------------------|------------------------------------|----------------------------------------------|
| Event                     | `<first token>.v<N>.<other tokens>` | `profiles.v<N>.<subject>`                   |
| Service method            | `<service>.$rpc.v<N>.<method>`     | `profiles.$rpc.v<N>.<service>.<method>`      |
| Persistent service method | `<service>.$persist.v<N>.<method>` | `profiles.$persist.v<N>.<service>.<method>`  |

`<N>` is the major version, the `.v<N>` at the end of the proto package. `<subject>` is the `subject` option of the
event, `<service>` the `name` option of the service, and `<method>` the `name` option of the method. Without these
options, surf-nats derives the names in kebab case (see [Names](../README.md#names)). A global event subject of one
token gets the version appended: `ready` travels on `ready.v1`.

The examples travel on these subjects:

| Contract                                         | Subject                      |
|--------------------------------------------------|------------------------------|
| Event `NameChanged`, `subject: "player.name-changed"` | `player.v1.name-changed` |
| Method `lookup` of the global service `roles`    | `roles.$rpc.v1.lookup`       |
| Persistent method `whitelist` of `roles`         | `roles.$persist.v1.whitelist` |

Subjects that contain `$` must be quoted in a shell: `'roles.$persist.v1.whitelist'`.

### Events

Publish the encoded event message to its subject. Delivery is not guaranteed: instances that are offline miss the
event.

Kotlin listeners can read where an event came from through `natsEventContext().origin`. surf-nats fills it from the
optional `Surf-Origin` header. Set the header to identify your application, for example `Surf-Origin:panel`.
Without it, `origin` is `null`.

To receive events, subscribe to the event subject and decode each body as the event message. Events from surf-nats
carry `Surf-Origin` as `<client name>@<instance id>`.

### Calling a service

The body of a call is the encoded request message of the `rpc`. For `lookup`, that is a `LookupRequest` with
`user_id` set.

How you send a call depends on the method:

| Persistent | Recipients      | Response                | How to call it                                                             |
|------------|-----------------|-------------------------|----------------------------------------------------------------------------|
| no         | `ANY_INSTANCE`  | anything                | NATS request, wait for the reply from one instance                         |
| no         | `ALL_INSTANCES` | `google.protobuf.Empty` | NATS publish, no reply                                                     |
| no         | `ALL_INSTANCES` | a message               | NATS publish with a reply subject, collect one reply per instance          |
| yes        | `ANY_INSTANCE`  | `google.protobuf.Empty` | JetStream publish, wait for the publish acknowledgement                    |
| yes        | `ANY_INSTANCE`  | a message               | JetStream publish with `Surf-Reply-To`, wait for the reply on that subject |

`ANY_INSTANCE` is the default. Persistent methods never use `ALL_INSTANCES`: the Gradle plugin rejects that
combination.

#### Calls to one instance

Send a NATS request to the method's subject, for example `roles.$rpc.v1.lookup`. NATS picks one instance, and its
reply comes back to your request.

If no instance serves the method, NATS answers at once with a "no responders" status. If the method takes longer
than your request timeout, you get no reply.

#### Reading a reply

Check the headers of a reply in this order:

| Reply                         | Meaning                                                                                                              |
|-------------------------------|----------------------------------------------------------------------------------------------------------------------|
| Header `Surf-Error`           | The method threw. The header holds the exception class, the body the message as UTF-8 text, at most 4096 characters. |
| Header `Surf-Payload-Ref`     | The result was too large for one message. Load it from the object store (see [Large payloads](#large-payloads)).    |
| Header `Surf-Answer: unknown` | The method answered `Answer.Unknown`. The body is empty.                                                             |
| Any other reply               | The encoded response message, or an empty body for `google.protobuf.Empty`.                                           |

A response message whose fields are all unset also has an empty body. The response type of the `rpc` tells you how
to read it.

#### Calls to all instances without a result

Publish the call without a reply subject. Every instance runs the method, and you get no feedback.

#### Calls to all instances with a result

Every instance answers once, with a known value or with unknown. Collect the replies like this:

1. Subscribe to a reply subject of your own, for example a new inbox from your NATS client.
2. Publish the call with that reply subject.
3. Read the replies as described under [Reading a reply](#reading-a-reply). The first reply with a value is the
   answer, and you can stop listening.
4. Stop after a timeout you choose, for example 5 seconds, the default of Kotlin callers. If no reply had a value,
   the answer is unknown. If some instances threw and none had a value, treat the call as failed.

#### Persistent methods without a result

A persistent call goes to JetStream, which stores it and confirms the storage with a publish acknowledgement. Send
it as a NATS request, because a request to a stream subject returns that acknowledgement, and add a `Nats-Msg-Id`
header, for example `panel-whitelist-42-1718000000`.

The acknowledgement looks like `{"stream":"SURF__ROLES_V1","seq":17}`. Only that acknowledgement means that the call
is stored. If you get an error or no acknowledgement, the call may not be stored. Send it again with the same
`Nats-Msg-Id`.

JetStream discards a message whose `Nats-Msg-Id` it has seen within the last two minutes, so a repeated call is
stored once. Send repeats within those two minutes. A repeat sent later is stored as a second call, so protect the
operation with a key of your own if it must not run twice. Use a new `Nats-Msg-Id` for each new call.

After the acknowledgement, one instance runs the call. If no instance is running, the call waits until one starts.
If the method throws, surf-nats retries it several times and then moves the call to the dead-letter stream (see
[Dead letters](#dead-letters)).

#### Persistent methods with a result

Publish the call to JetStream as described for persistent methods without a result, including the `Nats-Msg-Id`.
Add these headers:

| Header          | Required | Value                                                                                              |
|-----------------|----------|----------------------------------------------------------------------------------------------------|
| `Surf-Reply-To` | yes      | A reply subject you subscribed to before publishing                                                |
| `Surf-Deadline` | no       | The time after which you no longer need the result (see [Deadlines](#deadlines))                   |
| `Nats-TTL`      | no       | How long JetStream keeps the stored call, for example `60s`. Kotlin callers send their timeout.    |

The instance that runs the call publishes the reply to the subject in `Surf-Reply-To` as a plain NATS message. Read
it as described under [Reading a reply](#reading-a-reply). JetStream does not store the reply: if you stop
listening, the reply is lost.

Without `Surf-Reply-To`, the call still runs, but nobody receives the result.

### Deadlines

A call may carry a `Surf-Deadline` header: a point in time in milliseconds since the Unix epoch. An instance drops a
call whose deadline has passed, without running it. Without the header, the instance gives the call the method's
timeout (see [Timeouts](../README.md#timeouts)), counted from the moment the call arrives. Kotlin callers send no
deadline with persistent calls without a result, because these wait in the stream until an instance runs them.

Deadlines compare the clocks of different hosts. Keep the clock of your application synchronized (NTP).

### Large payloads

A message holds at most `max_payload` bytes, 1 MB by default. Larger arguments and results travel through the NATS
object store. The message then has an empty body and a `Surf-Payload-Ref` header with the name of an object that
holds the body.

To send large arguments, store the encoded request message as an object in the right bucket, set
`Surf-Payload-Ref` to the object name, and send an empty body:

| Method                                         | Bucket                          |
|------------------------------------------------|---------------------------------|
| Persistent, response `google.protobuf.Empty`   | `surf-nats-persistent-payloads` |
| All other methods                              | `surf-nats-payloads`            |

To receive a large result, load the object named in `Surf-Payload-Ref` from `surf-nats-payloads`, decode it as the
response message, and delete it. Objects that nobody deletes expire: after 10 minutes in `surf-nats-payloads` and
after 7 days in `surf-nats-persistent-payloads`.

### Errors

| Problem                                               | Event                        | Non-persistent call     | Persistent, without a result | Persistent, with a result |
|-------------------------------------------------------|------------------------------|-------------------------|------------------------------|---------------------------|
| Missing or unknown `Content-Type`                     | dropped, logged as a warning | reply with `Surf-Error` | dead letter at once          | reply with `Surf-Error`   |
| Body cannot be decoded or lacks a required field      | dropped, logged as a warning | reply with `Surf-Error` | dead letter at once          | reply with `Surf-Error`   |
| Method name unknown                                   | not applicable               | no responders           | retried, then dead letter    | retried, then dead letter |
| Method throws                                         | not applicable               | reply with `Surf-Error` | retried, then dead letter    | reply with `Surf-Error`   |
| Deadline passed                                       | not applicable               | dropped                 | dropped                      | dropped                   |

### Dead letters

A dead letter is a persistent call that surf-nats gave up on: one that could not be decoded, or one that failed
every retry. surf-nats moves it to the dead-letter stream of the service and does not touch it again. The stream is
named `SURF__<SERVICE>_V<N>_DLQ` for a global service, in upper case with hyphens replaced by underscores, for
example `SURF__ROLES_V1_DLQ`. Inspect it with the `nats` CLI:

```sh
nats stream view SURF__ROLES_V1_DLQ
```

A dead letter keeps the body and all headers of the call except the `Nats-*` headers, and gains two headers:

| Header              | Value                                            |
|---------------------|--------------------------------------------------|
| `Surf-Dead-Subject` | The subject the call was originally published to |
| `Surf-Error`        | The exception class that made the call fail      |

To run the call again, fix the cause and publish the body to the subject in `Surf-Dead-Subject`, as described under
[Persistent methods without a result](#persistent-methods-without-a-result). Use the original `Content-Type` and a
new `Nats-Msg-Id`. Do not copy `Surf-Error`, `Surf-Dead-Subject`, or an old `Surf-Deadline` into the new message.
Keep `Surf-Payload-Ref` if present: its object lives for 7 days from the original call.

### Header reference

| Header                | You send it | You receive it | Meaning                                                                    |
|-----------------------|-------------|----------------|----------------------------------------------------------------------------|
| `Content-Type`        | yes         | yes            | `application/protobuf`. Required.                                          |
| `Nats-Msg-Id`         | yes         | no             | Id of a persistent call. JetStream discards repeats within two minutes.   |
| `Nats-TTL`            | yes         | no             | Lifetime of a stored persistent call with a result. Optional.              |
| `Surf-Reply-To`       | yes         | no             | Reply subject for the result of a persistent call.                         |
| `Surf-Deadline`       | yes         | no             | Deadline in milliseconds since the Unix epoch. Optional.                   |
| `Surf-Origin`         | yes         | yes            | Identifies the publisher of an event. Optional.                            |
| `Surf-Payload-Ref`    | yes         | yes            | Object in the object store that holds the body.                            |
| `Surf-Sender-Version` | no          | yes            | surf-nats version of the sender, for diagnostics.                          |
| `Surf-Error`          | no          | yes            | Exception class of a failed call. The body holds the message.              |
| `Surf-Answer`         | no          | yes            | `unknown` for `Answer.Unknown`.                                            |
| `Surf-Dead-Subject`   | no          | yes            | Original subject of a dead letter.                                         |
