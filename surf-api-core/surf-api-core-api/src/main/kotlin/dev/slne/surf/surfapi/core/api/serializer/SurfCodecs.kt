package dev.slne.surf.surfapi.core.api.serializer

import dev.slne.surf.surfapi.core.api.serializer.adventure.book.AdventureBookCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.bossbar.AdventureBossBarCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.AdventureComponentCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.decoration.AdventureTextDecorationCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.shadowcolor.AdventureShadowColorCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.textcolor.AdventureTextColorCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.key.AdventureKeyCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.resourcepack.info.AdventureResourcePackInfoCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.sound.AdventureSoundCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.sound.stop.AdventureSoundStopCodec
import dev.slne.surf.surfapi.core.api.serializer.adventure.title.AdventureTitleCodec
import dev.slne.surf.surfapi.core.api.serializer.java.JavaDurationCodec
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.date.date.DateCodec
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.date.local.LocalDateCodec
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.instant.InstantCodec
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.ldt.LocalDateTimeCodec
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.time.local.LocalTimeCodec
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.zone.id.ZoneIdCodec
import dev.slne.surf.surfapi.core.api.serializer.java.datetime.zone.offset.ZoneOffsetCodec
import dev.slne.surf.surfapi.core.api.serializer.java.ip.inet.InetAddressCodec
import dev.slne.surf.surfapi.core.api.serializer.java.ip.inetsocket.InetSocketAddressCodec
import dev.slne.surf.surfapi.core.api.serializer.java.ip.ipv4.Inet4AddressCodec
import dev.slne.surf.surfapi.core.api.serializer.java.ip.ipv6.Inet6AddressCodec
import dev.slne.surf.surfapi.core.api.serializer.java.uri.URICodec
import dev.slne.surf.surfapi.core.api.serializer.java.url.URLCodec
import dev.slne.surf.surfapi.core.api.serializer.java.uuid.JavaUUIDCodec
import dev.slne.surf.surfapi.core.api.serializer.kotlin.KotlinDurationCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m2d.SpongeMatrix2dCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m2f.SpongeMatrix2fCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m3d.SpongeMatrix3dCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m3f.SpongeMatrix3fCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m4d.SpongeMatrix4dCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.m4f.SpongeMatrix4fCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.mnd.SpongeMatrixNdCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.matrix.mnf.SpongeMatrixNfCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.quaternion.qnd.SpongeQuaterniondCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.quaternion.qnf.SpongeQuaternionfCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v2d.SpongeVector2dCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v2f.SpongeVector2fCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v2i.SpongeVector2iCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v2l.SpongeVector2lCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v3d.SpongeVector3dCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v3f.SpongeVector3fCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v3i.SpongeVector3iCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v3l.SpongeVector3lCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v4d.SpongeVector4dCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v4f.SpongeVector4fCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v4i.SpongeVector4iCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.v4l.SpongeVector4lCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.vnd.SpongeVectorNdCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.vnf.SpongeVectorNfCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.vni.SpongeVectorNiCodec
import dev.slne.surf.surfapi.core.api.serializer.spongepowered.math.vector.vnl.SpongeVectorNlCodec

object SurfCodecs {
    // region Adventure
    val COMPONENT = AdventureComponentCodec.CODEC
    val SHADOW_COLOR = AdventureShadowColorCodec.CODEC
    val TEXT_COLOR = AdventureTextColorCodec.CODEC
    val TEXT_DECORATION = AdventureTextDecorationCodec.CODEC
    val BOOK = AdventureBookCodec.CODEC
    val BOSS_BAR = AdventureBossBarCodec.CODEC
    val RESOURCE_PACK_INFO = AdventureResourcePackInfoCodec.CODEC
    val KEY = AdventureKeyCodec.CODEC
    val SOUND = AdventureSoundCodec.CODEC
    val SOUND_STOP = AdventureSoundStopCodec.CODEC
    val TITLE = AdventureTitleCodec.CODEC
    // endregion

    // region SpongePowered Math
    val SPONGE_VECTOR_2D = SpongeVector2dCodec.CODEC
    val SPONGE_VECTOR_2F = SpongeVector2fCodec.CODEC
    val SPONGE_VECTOR_2I = SpongeVector2iCodec.CODEC
    val SPONGE_VECTOR_2L = SpongeVector2lCodec.CODEC
    val SPONGE_VECTOR_3D = SpongeVector3dCodec.CODEC
    val SPONGE_VECTOR_3F = SpongeVector3fCodec.CODEC
    val SPONGE_VECTOR_3I = SpongeVector3iCodec.CODEC
    val SPONGE_VECTOR_3L = SpongeVector3lCodec.CODEC
    val SPONGE_VECTOR_4D = SpongeVector4dCodec.CODEC
    val SPONGE_VECTOR_4F = SpongeVector4fCodec.CODEC
    val SPONGE_VECTOR_4I = SpongeVector4iCodec.CODEC
    val SPONGE_VECTOR_4L = SpongeVector4lCodec.CODEC
    val SPONGE_VECTOR_ND = SpongeVectorNdCodec.CODEC
    val SPONGE_VECTOR_NF = SpongeVectorNfCodec.CODEC
    val SPONGE_VECTOR_NI = SpongeVectorNiCodec.CODEC
    val SPONGE_VECTOR_NL = SpongeVectorNlCodec.CODEC
    val SPONGE_MATRIX_2D = SpongeMatrix2dCodec.CODEC
    val SPONGE_MATRIX_2F = SpongeMatrix2fCodec.CODEC
    val SPONGE_MATRIX_3D = SpongeMatrix3dCodec.CODEC
    val SPONGE_MATRIX_3F = SpongeMatrix3fCodec.CODEC
    val SPONGE_MATRIX_4D = SpongeMatrix4dCodec.CODEC
    val SPONGE_MATRIX_4F = SpongeMatrix4fCodec.CODEC
    val SPONGE_MATRIX_ND = SpongeMatrixNdCodec.CODEC
    val SPONGE_MATRIX_NF = SpongeMatrixNfCodec.CODEC
    val SPONGE_QUATERNION_F = SpongeQuaternionfCodec.CODEC
    val SPONGE_QUATERNION_D = SpongeQuaterniondCodec.CODEC
    // endregion

    // region Java
    val URI = URICodec.CODEC
    val URL = URLCodec.CODEC
    val UUID = JavaUUIDCodec.CODEC
    val DATE = DateCodec.CODEC
    val LOCAL_DATE = LocalDateCodec.CODEC
    val INSTANT = InstantCodec.CODEC
    val LOCAL_DATE_TIME = LocalDateTimeCodec.CODEC
    val LOCAL_TIME = LocalTimeCodec.CODEC
    val ZONE_ID = ZoneIdCodec.CODEC
    val ZONE_OFFSET = ZoneOffsetCodec.CODEC
    val INET_ADDRESS = InetAddressCodec.CODEC
    val INET_SOCKET_ADDRESS = InetSocketAddressCodec.CODEC
    val INET4_ADDRESS = Inet4AddressCodec.CODEC
    val INET6_ADDRESS = Inet6AddressCodec.CODEC
    val JAVA_DURATION = JavaDurationCodec.CODEC
    // endregion

    // region Kotlin
    val KOTLIN_DURATION = KotlinDurationCodec.CODEC
    // endregion
}