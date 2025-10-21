package dev.slne.surf.surfapi.core.api.serializer

import dev.slne.surf.surfapi.core.api.serializer.adventure.book.AdventureBookSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.bossbar.AdventureBossBarSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.AdventureComponentSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.decoration.AdventureTextDecorationSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.shadowcolor.AdventureShadowColorSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.textcolor.AdventureTextColorSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.key.AdventureKeySerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.resourcepack.info.AdventureResourcePackInfoSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.sound.AdventureSoundSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.sound.stop.AdventureSoundStopSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.title.AdventureTitleSerializer
import dev.slne.surf.surfapi.core.api.serializer.adventure.title.AdventureTitleSerializer.AdventureTitleTimes
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.date.date.DateSerializer
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.date.local.LocalDateSerializer
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.instant.InstantSerializer
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.ldt.LocalDateTimeSerializer
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.offset.OffsetDateTimeSerializer
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.zdt.ZonedDateTimeSerializer
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.time.local.LocalTimeSerializer
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.zone.id.ZonedIdSerializer
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.zone.offset.ZoneOffsetSerializer
import dev.slne.surf.surfapi.core.api.serializer.java.ip.inet.InetAddressSerializer
import dev.slne.surf.surfapi.core.api.serializer.java.ip.inetsocket.InetSocketAddressSerializer
import dev.slne.surf.surfapi.core.api.serializer.java.ip.ipv4.Inet4AddressSerializer
import dev.slne.surf.surfapi.core.api.serializer.java.ip.ipv6.Inet6AddressSerializer
import dev.slne.surf.surfapi.core.api.serializer.java.uri.URISerializer
import dev.slne.surf.surfapi.core.api.serializer.java.url.URLSerializer
import dev.slne.surf.surfapi.core.api.serializer.java.uuid.JavaUUIDSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m2d.SpongeMatrix2dSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m2f.SpongeMatrix2fSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m3d.SpongeMatrix3dSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m3f.SpongeMatrix3fSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m4d.SpongeMatrix4dSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m4f.SpongeMatrix4fSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.mnd.SpongeMatrixNdSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.mnf.SpongeMatrixNfSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.quaternion.qnd.SpongeQuaterniondSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.quaternion.qnf.SpongeQuaternionfSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v2d.SpongeVector2dSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v2f.SpongeVector2fSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v2i.SpongeVector2iSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v2l.SpongeVector2lSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v3d.SpongeVector3dSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v3f.SpongeVector3fSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v3i.SpongeVector3iSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v3l.SpongeVector3lSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v4d.SpongeVector4dSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v4f.SpongeVector4fSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v4i.SpongeVector4iSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v4l.SpongeVector4lSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.vnd.SpongeVectorNdSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.vnf.SpongeVectorNfSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.vni.SpongeVectorNiSerializer
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.vnl.SpongeVectorNlSerializer
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
        contextual(AdventureTitleTimes)
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
        contextual(DateSerializer)
        contextual(LocalDateSerializer)
        contextual(InstantSerializer)
        contextual(LocalDateTimeSerializer)
        contextual(OffsetDateTimeSerializer)
        contextual(ZonedDateTimeSerializer)
        contextual(LocalTimeSerializer)
        contextual(ZonedIdSerializer)
        contextual(ZoneOffsetSerializer)
        contextual(InetAddressSerializer)
        contextual(InetSocketAddressSerializer)
        contextual(Inet4AddressSerializer)
        contextual(Inet6AddressSerializer)
        contextual(URLSerializer)
    }

    val all = SerializersModule {
        include(adventure)
        include(java)
        include(spongePoweredMath)
    }
}