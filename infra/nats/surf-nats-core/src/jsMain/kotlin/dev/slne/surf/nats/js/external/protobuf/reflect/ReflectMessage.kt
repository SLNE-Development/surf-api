@file:JsModule("@bufbuild/protobuf/reflect")

package dev.slne.surf.nats.js.external.protobuf.reflect

import dev.slne.surf.nats.js.ProtoMessage
import dev.slne.surf.nats.js.external.protobuf.DescField
import dev.slne.surf.nats.js.external.protobuf.DescFieldEnum
import dev.slne.surf.nats.js.external.protobuf.DescFieldEnumList
import dev.slne.surf.nats.js.external.protobuf.DescFieldEnumMap
import dev.slne.surf.nats.js.external.protobuf.DescFieldMessage
import dev.slne.surf.nats.js.external.protobuf.DescFieldMessageList
import dev.slne.surf.nats.js.external.protobuf.DescFieldMessageMap
import dev.slne.surf.nats.js.external.protobuf.DescFieldScalar
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.DescOneof
import dev.slne.surf.nats.js.external.protobuf.UnknownField

// Reflection access to a message (reflect-types.d.ts); get is overloaded per field variant of ReflectMessageGet.
internal external interface ReflectMessage {
    val message: ProtoMessage
    val desc: DescMessage
    val fields: Array<DescField>

    fun isSet(field: DescField): Boolean
    fun clear(field: DescField)
    fun oneofCase(oneof: DescOneof): DescField?

    // ScalarValue: number, bigint, string, boolean or Uint8Array by the scalar type
    fun get(field: DescFieldScalar): Any
    fun get(field: DescFieldEnum): Int
    fun get(field: DescFieldMessage): ReflectMessage
    fun get(field: DescFieldEnumList): ReflectList<Int>
    fun get(field: DescFieldMessageList): ReflectList<ReflectMessage>
    fun get(field: DescFieldEnumMap): ReflectMap<Any, Int>
    fun get(field: DescFieldMessageMap): ReflectMap<Any, ReflectMessage>

    fun set(field: DescFieldEnum, value: Int)

    fun getUnknown(): Array<UnknownField>?

    // Declared as UnknownField[]; the value is assigned to $unknown as is, so undefined removes the unknown fields.
    fun setUnknown(value: Array<UnknownField>?)
}
