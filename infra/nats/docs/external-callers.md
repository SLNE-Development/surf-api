# External callers

This guide is for applications that are not built with surf-nats but exchange messages with applications
that are, for example a web panel written in TypeScript. It describes the messages your application sends
and receives. Any NATS client library works, and the examples use the
[`nats` CLI](https://github.com/nats-io/natscli).

Read the [README](../README.md) first for the concepts: clients, events, services, and persistent methods.

## Before you start

Your application has no surf-nats client name. Prepare the Kotlin side so that it can reach the events and
services it needs:

- Mark the events and services `global = true`. The subject of a client-scoped event or service contains
  the client name, so your application would depend on that name.
- Set every name explicitly: `subject` on `@NatsEvent`, the name on `@NatsService`, `@NatsMethod` on each
  method, and `@NatsParam` on each parameter. Derived names change when someone renames Kotlin code, and
  your application breaks without a compile error.
- Annotate events that your application receives with `@NatsFormat(WireFormat.JSON)`. Kotlin publishers
  send CBOR otherwise.
- Register a persistent service once before your application calls it. Registration creates the JetStream
  stream, and publishing to a stream that does not exist fails.

How you call a method depends on whether it is persistent, which instances it addresses, and what it
returns (see [What a call does](../README.md#what-a-call-does)). The Kotlin interface shows all three: look
for `@NatsPersistent`, `@NatsRecipients`, and the return type.

## Message format

Every message carries a `Content-Type` header. surf-nats accepts two values:

| `Content-Type` | Format |
| --- | --- |
| `application/json` | JSON, the right choice for your application |
| `application/cbor` | CBOR, the default of Kotlin callers |

A message without a `Content-Type`, or with another value, fails. What happens then depends on the kind of
message (see [Errors](#errors)). Header names are case-sensitive: `content-type` is not `Content-Type`.

JSON follows these rules:

- Field names are in snake_case. A field `userId` in Kotlin is `user_id` in JSON. This also applies to
  names set with `@NatsParam`: `@NatsParam("userId")` is `user_id` in JSON.
- Enum values are the names of the Kotlin constants, for example `"ADD"`.
- UUIDs are strings, for example `"0b6f1c2e-6d4f-4d1a-9f0e-2c3b1a7e5d90"`.
- Unknown fields are ignored, so a newer sender can add fields.
- A field of a Kotlin class that has a default value may be omitted. A nullable field without a default
  must be present, as `null` if it has no value. Method parameters follow a different rule, described under
  [Calling a service](#calling-a-service).

## Subjects

NATS delivers each message by its subject. surf-nats builds subjects as follows:

| What | Global | Client-scoped (client `profiles`) |
| --- | --- | --- |
| Event | `<event-subject>` | `profiles.<event-subject>` |
| Service method | `<service>.$rpc.<method>` | `profiles.$rpc.<service>.<method>` |
| Persistent service method | `<service>.$persist.<method>` | `profiles.$persist.<service>.<method>` |

`<event-subject>` is the `subject` value of `@NatsEvent`, `<service>` the name in `@NatsService`, and
`<method>` the name in `@NatsMethod`. These names appear in the subject exactly as written in Kotlin.
snake_case applies only to JSON field names.

Subjects that contain `$` must be quoted in a shell: `'roles.$persist.whitelist'`.

## Events

The examples in this section use this event:

```kotlin
@NatsEvent(subject = "player.name-changed", global = true)
@NatsFormat(WireFormat.JSON)
data class NameChanged(val playerId: String, val name: String)
```

### Publishing events

Publish the event object as JSON to its subject:

```sh
nats pub 'player.name-changed' '{"player_id":"alice","name":"Alice"}' \
  -H 'Content-Type:application/json'
```

The body is the event itself, without an envelope. Delivery is not guaranteed: instances that are offline
miss the event.

Kotlin listeners can read where an event came from through `natsEventContext().origin`. surf-nats fills it
from the optional `Surf-Origin` header. Set the header to identify your application, for example
`Surf-Origin:panel`. Without it, `origin` is `null`.

### Receiving events

Subscribe to the event subject and decode each body by its `Content-Type`. The body has the same shape as
the one you publish.

## Calling a service

The examples in this section use this service:

```kotlin
@NatsService("roles", global = true)
interface RolesService {
    @NatsMethod("lookup")
    suspend fun lookup(@NatsParam("userId") userId: Long): String?
}
```

The body of a call is a JSON object with one field per parameter, named by `@NatsParam` in snake_case:

```json
{"user_id": 42}
```

A method without parameters takes `{}`. A nullable parameter may be omitted and becomes `null`. Any other
missing parameter fails the call. This differs from fields of Kotlin classes, where a nullable field
without a default must be present.

How you send a call depends on the method:

| Persistent | Recipients | Returns | How to call it |
| --- | --- | --- | --- |
| no | `ANY_INSTANCE` | anything | NATS request, wait for the reply from one instance |
| no | `ALL_INSTANCES` | `Unit` | NATS publish, no reply |
| no | `ALL_INSTANCES` | `Answer<T>` | NATS publish with a reply subject, collect one reply per instance |
| yes | `ANY_INSTANCE` | `Unit` | JetStream publish, wait for the publish acknowledgement |
| yes | `ANY_INSTANCE` | a value | JetStream publish with `Surf-Reply-To`, wait for the reply on that subject |

`ANY_INSTANCE` is the default. Persistent methods never use `ALL_INSTANCES`: surf-nats rejects that
combination.

### Calls to one instance

Send a NATS request to the method's subject. NATS picks one instance, and its reply comes back to your
request:

```sh
nats req 'roles.$rpc.lookup' '{"user_id":42}' -H 'Content-Type:application/json'
```

If no instance serves the method, NATS answers at once with a "no responders" status. If the method takes
longer than your request timeout, you get no reply.

### Reading a reply

A reply has the same `Content-Type` as the call. Check its headers in this order:

| Reply | Meaning |
| --- | --- |
| Header `Surf-Error` | The method threw. The header holds the exception class, the body the message as plain text. |
| Header `Surf-Payload-Ref` | The result was too large for one message. Load it from the object store (see [Large payloads](#large-payloads)). |
| Header `Surf-Answer: unknown` | The method returned `Answer.Unknown`. The body is empty. |
| Empty body, none of the headers above | The method returned `Unit`. |
| Any other body | The return value as JSON, for example `"Alice"`, `42`, or `null`. |

### Calls to all instances that return `Unit`

Publish the call without a reply subject. Every instance runs the method, and you get no feedback:

```sh
nats pub 'players.$rpc.broadcast' '{"message":"Restart in 5 minutes"}' -H 'Content-Type:application/json'
```

### Calls to all instances that return `Answer<T>`

Every instance answers once, with a known value or with unknown. Collect the replies like this:

1. Subscribe to a reply subject of your own, for example a new inbox from your NATS client.
2. Publish the call with that reply subject.
3. Read the replies as described under [Reading a reply](#reading-a-reply). The first reply with a value
   is the answer, and you can stop listening.
4. Stop after a timeout you choose, for example 5 seconds, the default of Kotlin callers. If no reply had a
   value, the answer is unknown. If some instances threw and none had a value, treat the call as failed.

### Persistent methods that return `Unit`

A persistent call goes to JetStream, which stores it and confirms the storage with a publish
acknowledgement. Send it as a NATS request, because a request to a stream subject returns that
acknowledgement. Add a `Nats-Msg-Id` header:

```sh
nats req 'roles.$persist.whitelist' '{"action":"ADD","user_id":42}' \
  -H 'Content-Type:application/json' \
  -H 'Nats-Msg-Id:panel-whitelist-42-1718000000'
```

The acknowledgement looks like `{"stream":"SURF__ROLES","seq":17}`. Only that acknowledgement means that
the call is stored. If you get an error or no acknowledgement, the call may not be stored. Send it again
with the same `Nats-Msg-Id`.

JetStream discards a message whose `Nats-Msg-Id` it has seen within the last two minutes, so a repeated call
is stored once. Send repeats within those two minutes. A repeat sent later is stored as a second call, so
protect the operation with a key of your own if it must not run twice. Use a new `Nats-Msg-Id` for each new
call.

After the acknowledgement, one instance runs the call. If no instance is running, the call waits until one
starts. If the method throws, surf-nats retries it several times and then moves the call to the
dead-letter stream (see [Dead letters](#dead-letters)).

### Persistent methods that return a value

Publish the call to JetStream as described for persistent methods that return `Unit`, including the
`Nats-Msg-Id`. Add these headers:

| Header | Required | Value |
| --- | --- | --- |
| `Surf-Reply-To` | yes | A reply subject you subscribed to before publishing |
| `Surf-Deadline` | no | The time after which you no longer need the result (see [Deadlines](#deadlines)) |

The instance that runs the call publishes the reply to the subject in `Surf-Reply-To` as a plain NATS
message. Read it as described under [Reading a reply](#reading-a-reply). JetStream does not store the
reply: if you stop listening, the reply is lost.

Without `Surf-Reply-To`, the call still runs, but nobody receives the result.

## Deadlines

Every call may carry a `Surf-Deadline` header: a point in time in milliseconds since the Unix epoch. An
instance drops a call whose deadline has passed, without running it. Without the header, the instance
gives the call the method's timeout (see [Timeouts](../README.md#timeouts)), counted from the moment the
call arrives.

Deadlines compare the clocks of different hosts. Keep the clock of your application synchronized (NTP).

## Large payloads

A message holds at most `max_payload` bytes, 1 MB by default. Larger arguments and results travel through
the NATS object store. The message then has an empty body and a `Surf-Payload-Ref` header with the name of
an object that holds the body.

To send large arguments, store them as an object in the right bucket, set `Surf-Payload-Ref` to the object
name, and send an empty body:

| Method | Bucket |
| --- | --- |
| Persistent, returns `Unit` | `surf-nats-persistent-payloads` |
| All other methods | `surf-nats-payloads` |

To receive a large result, load the object named in `Surf-Payload-Ref` from `surf-nats-payloads`, decode
it by the reply's `Content-Type`, and delete it. Objects that nobody deletes expire: after 10 minutes in
`surf-nats-payloads` and after 7 days in `surf-nats-persistent-payloads`.

## Errors

| Problem | Event | Non-persistent call | Persistent call |
| --- | --- | --- | --- |
| Missing or unknown `Content-Type` | dropped, logged as a warning | reply with `Surf-Error` | dead letter at once |
| Body does not match the parameters | dropped, logged as a warning | reply with `Surf-Error` | dead letter at once |
| Method name unknown | not applicable | no responders | retried, then dead letter |
| Method throws | not applicable | reply with `Surf-Error` | returns `Unit`: retried, then dead letter. Returns a value: reply with `Surf-Error`. |
| Deadline passed | not applicable | dropped | dropped |

## Dead letters

A dead letter is a persistent call that surf-nats gave up on: one that could not be decoded, or one that
failed every retry. surf-nats moves it to the dead-letter stream of the service and does not touch it
again. The stream is named `SURF__<SERVICE>_DLQ` for a global service, in upper case with hyphens replaced
by underscores, for example `SURF__ROLES_DLQ`. Inspect it with the `nats` CLI:

```sh
nats stream view SURF__ROLES_DLQ
```

A dead letter keeps the body and all headers of the call except the `Nats-*` headers, and gains two
headers:

| Header | Value |
| --- | --- |
| `Surf-Dead-Subject` | The subject the call was originally published to |
| `Surf-Error` | The exception class that made the call fail |

To run the call again, fix the cause and publish the body to the subject in `Surf-Dead-Subject`, as
described under [Persistent methods that return `Unit`](#persistent-methods-that-return-unit). Use the
original `Content-Type` and a new `Nats-Msg-Id`. Do not copy `Surf-Error`, `Surf-Dead-Subject`, or an old
`Surf-Deadline` into the new message. Keep `Surf-Payload-Ref` if present: its object lives for 7 days from
the original call.

## Header reference

| Header | You send it | You receive it | Meaning |
| --- | --- | --- | --- |
| `Content-Type` | yes | yes | `application/json` or `application/cbor`. Required. |
| `Nats-Msg-Id` | yes | no | Id of a persistent call. JetStream discards repeats within two minutes. |
| `Surf-Reply-To` | yes | no | Reply subject for the result of a persistent call. |
| `Surf-Deadline` | yes | no | Deadline in milliseconds since the Unix epoch. Optional. |
| `Surf-Origin` | yes | yes | Identifies the publisher of an event. Optional. |
| `Surf-Payload-Ref` | yes | yes | Object in the object store that holds the body. |
| `Surf-Error` | no | yes | Exception class of a failed call. The body holds the message. |
| `Surf-Answer` | no | yes | `unknown` for `Answer.Unknown`. |
| `Surf-Dead-Subject` | no | yes | Original subject of a dead letter. |
