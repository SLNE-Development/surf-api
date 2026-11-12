package dev.slne.surf.nats.gradle.testkit

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Minimal ustar reader and writer for the archives the weaver registry serves.
 *
 * Entry names are written verbatim, so tests can create entries a real packer would never emit, such as `../x`.
 */
internal object TarGz {

    private const val BLOCK = 512
    private const val NUL = "\u0000"

    fun create(entries: Map<String, String>): ByteArray {
        val tar = ByteArrayOutputStream()
        for ((name, content) in entries) {
            val data = content.toByteArray(Charsets.UTF_8)
            tar.write(header(name, data.size))
            tar.write(data)
            tar.write(ByteArray((BLOCK - data.size % BLOCK) % BLOCK))
        }
        tar.write(ByteArray(BLOCK * 2))

        return ByteArrayOutputStream().also { out ->
            GZIPOutputStream(out).use { it.write(tar.toByteArray()) }
        }.toByteArray()
    }

    fun names(file: File): List<String> = names(file.readBytes())

    fun names(archive: ByteArray): List<String> {
        val tar = GZIPInputStream(ByteArrayInputStream(archive)).use { it.readBytes() }
        val names = mutableListOf<String>()
        var offset = 0
        while (offset + BLOCK <= tar.size) {
            val name = String(tar, offset, 100, Charsets.UTF_8).trimEnd(NUL[0])
            if (name.isEmpty()) break
            val size = String(tar, offset + 124, 11, Charsets.US_ASCII).trim(NUL[0], ' ').toInt(8)
            val type = tar[offset + 156]
            if (type == '0'.code.toByte() || type == 0.toByte()) names += name
            offset += BLOCK + (size + BLOCK - 1) / BLOCK * BLOCK
        }
        return names
    }

    private fun header(name: String, size: Int): ByteArray {
        val header = ByteArray(BLOCK)

        fun put(offset: Int, text: String) {
            text.toByteArray(Charsets.UTF_8).copyInto(header, offset)
        }

        put(0, name)
        put(100, "0000644$NUL")
        put(108, "0000000$NUL")
        put(116, "0000000$NUL")
        put(124, size.toString(8).padStart(11, '0') + NUL)
        put(136, "00000000000$NUL")
        put(148, "        ")
        header[156] = '0'.code.toByte()
        put(257, "ustar$NUL")
        put(263, "00")

        val checksum = header.sumOf { it.toInt() and 0xFF }
        put(148, checksum.toString(8).padStart(6, '0') + NUL + " ")
        return header
    }
}
