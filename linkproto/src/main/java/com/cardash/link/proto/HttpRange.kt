package com.cardash.link.proto

/** The byte range a video player asks for ("Range: bytes=500-" etc.), so seeking works. */
object HttpRange {
    /**
     * The requested bytes of a [size]-byte file, or null to send the whole file. Only single ranges
     * are supported (all players use those); an unsatisfiable range returns [LongRange.EMPTY].
     */
    fun parse(header: String?, size: Long): LongRange? {
        val spec = header?.trim()?.removePrefix("bytes=")?.takeIf { header.trim().startsWith("bytes=") } ?: return null
        if (spec.contains(',')) return null
        val (a, b) = spec.split('-', limit = 2).map { it.trim() }.let { it[0] to it.getOrElse(1) { "" } }
        val range = when {
            a.isEmpty() -> {
                // "bytes=-500": the last 500 bytes.
                val suffix = b.toLongOrNull() ?: return null
                maxOf(0, size - suffix)..<size
            }
            else -> {
                val start = a.toLongOrNull() ?: return null
                val end = b.toLongOrNull()?.coerceAtMost(size - 1) ?: (size - 1)
                start..end
            }
        }
        return if (range.isEmpty() || range.first >= size) LongRange.EMPTY else range
    }
}
