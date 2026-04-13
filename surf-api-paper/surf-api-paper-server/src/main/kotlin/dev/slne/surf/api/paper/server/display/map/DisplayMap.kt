package dev.slne.surf.api.paper.server.display.map

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMapData
import dev.slne.surf.api.paper.server.display.user.DisplayUser

class DisplayMap(
    val mapId: Int,
    data: ByteArray
) {
    private var _data = data

    val data
        get() = ByteArray(_data.size).apply {
            _data.copyInto(this)
        }

    fun createPacket(): WrapperPlayServerMapData {
        return WrapperPlayServerMapData(mapId, 0, false, false, null, 128, 128, 0, 0, _data)
    }

    fun update(user: DisplayUser) {
        user.sendPacket(createPacket())
    }

    fun updateData(newData: ByteArray) {
        _data = newData
    }
}
