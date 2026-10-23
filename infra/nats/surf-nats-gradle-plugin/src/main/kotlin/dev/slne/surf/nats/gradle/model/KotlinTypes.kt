package dev.slne.surf.nats.gradle.model

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

internal object KotlinTypes {
    private const val SERIALIZATION = "kotlinx.serialization"
    private const val PROTOBUF = "kotlinx.serialization.protobuf"
    private const val DESCRIPTOR = "dev.slne.surf.nats.descriptor"

    val SERIALIZABLE = ClassName(SERIALIZATION, "Serializable")
    val SERIAL_NAME = ClassName(SERIALIZATION, "SerialName")
    val K_SERIALIZER = ClassName(SERIALIZATION, "KSerializer")
    val EXPERIMENTAL_SERIALIZATION_API = ClassName(SERIALIZATION, "ExperimentalSerializationApi")
    val SERIAL_DESCRIPTOR = ClassName("$SERIALIZATION.descriptors", "SerialDescriptor")
    val PRIMITIVE_SERIAL_DESCRIPTOR = ClassName("$SERIALIZATION.descriptors", "PrimitiveSerialDescriptor")
    val PRIMITIVE_KIND = ClassName("$SERIALIZATION.descriptors", "PrimitiveKind")
    val ENCODER = ClassName("$SERIALIZATION.encoding", "Encoder")
    val DECODER = ClassName("$SERIALIZATION.encoding", "Decoder")

    val PROTO_NUMBER = ClassName(PROTOBUF, "ProtoNumber")
    val PROTO_TYPE = ClassName(PROTOBUF, "ProtoType")
    val PROTO_INTEGER_TYPE = ClassName(PROTOBUF, "ProtoIntegerType")
    val PROTO_ONE_OF = ClassName(PROTOBUF, "ProtoOneOf")
    val PROTO_PACKED = ClassName(PROTOBUF, "ProtoPacked")

    val NATS_GENERATED_API = ClassName("dev.slne.surf.nats", "NatsGeneratedApi")
    val NATS_EVENT = ClassName(DESCRIPTOR, "NatsEvent")
    val NATS_EVENT_DESCRIPTOR = ClassName(DESCRIPTOR, "NatsEventDescriptor")
    val NATS_SERVICE_DESCRIPTOR = ClassName(DESCRIPTOR, "NatsServiceDescriptor")
    val NATS_METHOD_DESCRIPTOR = ClassName(DESCRIPTOR, "NatsMethodDescriptor")
    val NATS_PARAMETER = ClassName(DESCRIPTOR, "NatsParameter")
    val NATS_RESULT_KIND = ClassName(DESCRIPTOR, "NatsResultKind")
    val RECIPIENTS = ClassName("dev.slne.surf.nats.rpc", "Recipients")
    val ANSWER = ClassName("dev.slne.surf.nats.rpc", "Answer")

    val CONTENT_EQUALS = MemberName(DESCRIPTOR, "natsContentEquals")
    val CONTENT_HASH_CODE = MemberName(DESCRIPTOR, "natsContentHashCode")
    val CONTENT_TO_STRING = MemberName(DESCRIPTOR, "natsContentToString")

    fun serializableWith(serializer: ClassName): AnnotationSpec =
        AnnotationSpec.builder(SERIALIZABLE).addMember("with = %T::class", serializer).build()
}
