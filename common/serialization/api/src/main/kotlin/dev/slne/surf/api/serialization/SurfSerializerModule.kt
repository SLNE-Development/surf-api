package dev.slne.surf.api.serialization

import dev.slne.surf.api.serialization.adventure.book.AdventureBookSerializer
import dev.slne.surf.api.serialization.adventure.bossbar.AdventureBossBarSerializer
import dev.slne.surf.api.serialization.adventure.component.AdventureComponentSerializer
import dev.slne.surf.api.serialization.adventure.component.decoration.AdventureTextDecorationSerializer
import dev.slne.surf.api.serialization.adventure.component.shadowcolor.AdventureShadowColorSerializer
import dev.slne.surf.api.serialization.adventure.component.textcolor.AdventureTextColorSerializer
import dev.slne.surf.api.serialization.adventure.key.AdventureKeySerializer
import dev.slne.surf.api.serialization.adventure.resourcepack.info.AdventureResourcePackInfoSerializer
import dev.slne.surf.api.serialization.adventure.sound.AdventureSoundSerializer
import dev.slne.surf.api.serialization.adventure.sound.stop.AdventureSoundStopSerializer
import dev.slne.surf.api.serialization.adventure.title.AdventureTitleSerializer
import dev.slne.surf.api.serialization.java.bitset.BitSetSerializer
import dev.slne.surf.api.serialization.java.crypt.publickey.PublicKeySerializer
import dev.slne.surf.api.serialization.java.datetime.date.date.DateSerializer
import dev.slne.surf.api.serialization.java.datetime.date.local.LocalDateSerializer
import dev.slne.surf.api.serialization.java.datetime.datetime.instant.InstantSerializer
import dev.slne.surf.api.serialization.java.datetime.datetime.ldt.LocalDateTimeSerializer
import dev.slne.surf.api.serialization.java.datetime.datetime.offset.OffsetDateTimeSerializer
import dev.slne.surf.api.serialization.java.datetime.datetime.zdt.ZonedDateTimeSerializer
import dev.slne.surf.api.serialization.java.datetime.time.local.LocalTimeSerializer
import dev.slne.surf.api.serialization.java.datetime.zone.id.ZonedIdSerializer
import dev.slne.surf.api.serialization.java.datetime.zone.offset.ZoneOffsetSerializer
import dev.slne.surf.api.serialization.java.ip.inet.InetAddressSerializer
import dev.slne.surf.api.serialization.java.ip.inetsocket.InetSocketAddressSerializer
import dev.slne.surf.api.serialization.java.ip.ipv4.Inet4AddressSerializer
import dev.slne.surf.api.serialization.java.ip.ipv6.Inet6AddressSerializer
import dev.slne.surf.api.serialization.java.number.bigdecimal.BigDecimalSerializer
import dev.slne.surf.api.serialization.java.number.bigint.BigIntegerSerializer
import dev.slne.surf.api.serialization.java.uri.URISerializer
import dev.slne.surf.api.serialization.java.url.URLSerializer
import dev.slne.surf.api.serialization.java.uuid.JavaUUIDSerializer
import dev.slne.surf.api.serialization.spongepowered.math.matrix.m2d.SpongeMatrix2dSerializer
import dev.slne.surf.api.serialization.spongepowered.math.matrix.m2f.SpongeMatrix2fSerializer
import dev.slne.surf.api.serialization.spongepowered.math.matrix.m3d.SpongeMatrix3dSerializer
import dev.slne.surf.api.serialization.spongepowered.math.matrix.m3f.SpongeMatrix3fSerializer
import dev.slne.surf.api.serialization.spongepowered.math.matrix.m4d.SpongeMatrix4dSerializer
import dev.slne.surf.api.serialization.spongepowered.math.matrix.m4f.SpongeMatrix4fSerializer
import dev.slne.surf.api.serialization.spongepowered.math.matrix.mnd.SpongeMatrixNdSerializer
import dev.slne.surf.api.serialization.spongepowered.math.matrix.mnf.SpongeMatrixNfSerializer
import dev.slne.surf.api.serialization.spongepowered.math.quaternion.qnd.SpongeQuaterniondSerializer
import dev.slne.surf.api.serialization.spongepowered.math.quaternion.qnf.SpongeQuaternionfSerializer
import dev.slne.surf.api.serialization.spongepowered.math.vector.v2d.SpongeVector2dSerializer
import dev.slne.surf.api.serialization.spongepowered.math.vector.v2f.SpongeVector2fSerializer
import dev.slne.surf.api.serialization.spongepowered.math.vector.v2i.SpongeVector2iSerializer
import dev.slne.surf.api.serialization.spongepowered.math.vector.v2l.SpongeVector2lSerializer
import dev.slne.surf.api.serialization.spongepowered.math.vector.v3d.SpongeVector3dSerializer
import dev.slne.surf.api.serialization.spongepowered.math.vector.v3f.SpongeVector3fSerializer
import dev.slne.surf.api.serialization.spongepowered.math.vector.v3i.SpongeVector3iSerializer
import dev.slne.surf.api.serialization.spongepowered.math.vector.v3l.SpongeVector3lSerializer
import dev.slne.surf.api.serialization.spongepowered.math.vector.v4d.SpongeVector4dSerializer
import dev.slne.surf.api.serialization.spongepowered.math.vector.v4f.SpongeVector4fSerializer
import dev.slne.surf.api.serialization.spongepowered.math.vector.v4i.SpongeVector4iSerializer
import dev.slne.surf.api.serialization.spongepowered.math.vector.v4l.SpongeVector4lSerializer
import dev.slne.surf.api.serialization.spongepowered.math.vector.vnd.SpongeVectorNdSerializer
import dev.slne.surf.api.serialization.spongepowered.math.vector.vnf.SpongeVectorNfSerializer
import dev.slne.surf.api.serialization.spongepowered.math.vector.vni.SpongeVectorNiSerializer
import dev.slne.surf.api.serialization.spongepowered.math.vector.vnl.SpongeVectorNlSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

object SurfSerializerModule {
    val adventure = SerializersModule {
        contextual(AdventureComponentSerializer)
        contextual(AdventureTextDecorationSerializer)
        contextual(AdventureShadowColorSerializer)
        contextual(AdventureTextColorSerializer)
        contextual(AdventureBookSerializer)
        contextual(AdventureBossBarSerializer)
        contextual(AdventureResourcePackInfoSerializer)
        contextual(AdventureKeySerializer)
        contextual(AdventureSoundSerializer)
        contextual(AdventureSoundStopSerializer)
        contextual(AdventureTitleSerializer)
        contextual(AdventureTitleSerializer.AdventureTitleTimes)
    }

    val spongePoweredMath = SerializersModule {
        contextual(SpongeVector2dSerializer)
        contextual(SpongeVector2fSerializer)
        contextual(SpongeVector2iSerializer)
        contextual(SpongeVector2lSerializer)
        contextual(SpongeVector3dSerializer)
        contextual(SpongeVector3fSerializer)
        contextual(SpongeVector3iSerializer)
        contextual(SpongeVector3lSerializer)
        contextual(SpongeVector4dSerializer)
        contextual(SpongeVector4fSerializer)
        contextual(SpongeVector4iSerializer)
        contextual(SpongeVector4lSerializer)
        contextual(SpongeVectorNdSerializer)
        contextual(SpongeVectorNfSerializer)
        contextual(SpongeVectorNiSerializer)
        contextual(SpongeVectorNlSerializer)

        contextual(SpongeMatrix2dSerializer)
        contextual(SpongeMatrix2fSerializer)
        contextual(SpongeMatrix3dSerializer)
        contextual(SpongeMatrix3fSerializer)
        contextual(SpongeMatrix4dSerializer)
        contextual(SpongeMatrix4fSerializer)
        contextual(SpongeMatrixNdSerializer)
        contextual(SpongeMatrixNfSerializer)
        contextual(SpongeQuaterniondSerializer)
        contextual(SpongeQuaternionfSerializer)
    }

    val java = SerializersModule {
        contextual(URISerializer)
        contextual(JavaUUIDSerializer)
        contextual(InetAddressSerializer)
        contextual(InetSocketAddressSerializer)
        contextual(Inet4AddressSerializer)
        contextual(Inet6AddressSerializer)
        contextual(URLSerializer)
        contextual(BigIntegerSerializer)
        contextual(BigDecimalSerializer)
        contextual(BitSetSerializer)
    }

    val javaTime = SerializersModule {
        contextual(DateSerializer)
        contextual(LocalDateSerializer)
        contextual(InstantSerializer)
        contextual(LocalDateTimeSerializer)
        contextual(OffsetDateTimeSerializer)
        contextual(ZonedDateTimeSerializer)
        contextual(LocalTimeSerializer)
        contextual(ZonedIdSerializer)
        contextual(ZoneOffsetSerializer)
    }

    val javaCrypt = SerializersModule {
        contextual(PublicKeySerializer)
    }

    val all = SerializersModule {
        include(adventure)
        include(java)
        include(javaTime)
        include(javaCrypt)
        include(spongePoweredMath)
    }
}