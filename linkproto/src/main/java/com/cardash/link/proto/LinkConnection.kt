package com.cardash.link.proto

import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object LinkProtocol {
    const val PORT = 47819
    /** The car pings this often; either side drops a connection that's silent for [TIMEOUT_MS]. */
    const val PING_MS = 15_000L
    const val TIMEOUT_MS = 45_000
    /** Logging in (challenge and answer) must finish within this, so idle connections can't pile up. */
    const val HANDSHAKE_TIMEOUT_MS = 10_000
    /** Longest line accepted (album art is the big one, ~30 KB); anything larger ends the connection. */
    private const val MAX_LINE = 512 * 1024

    private val random = SecureRandom()

    fun randomHex(bytes: Int): String = ByteArray(bytes).also(random::nextBytes).toHex()

    /** A 6-digit pairing code. */
    fun pairCode(): String = "%06d".format(random.nextInt(1_000_000))

    fun mac(token: String, nonce: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(token.toByteArray(), "HmacSHA256"))
        return mac.doFinal(nonce.toByteArray()).toHex()
    }

    /**
     * Both sides prove they hold the same secret (the pairing token, or the code while pairing),
     * each over both sides' random nonces. The "car"/"phone" labels keep one side's answer from
     * being replayed as the other's.
     */
    fun carProof(secret: String, phoneNonce: String, carNonce: String): String = mac(secret, "car|$phoneNonce|$carNonce")

    fun phoneProof(secret: String, phoneNonce: String, carNonce: String): String = mac(secret, "phone|$phoneNonce|$carNonce")

    /** Constant-time compare, so the answer can't be guessed byte by byte from timing. */
    fun sameMac(a: String, b: String): Boolean = MessageDigest.isEqual(a.toByteArray(), b.toByteArray())

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    internal fun readLine(reader: BufferedReader): String? {
        val sb = StringBuilder()
        while (true) {
            val c = reader.read()
            if (c == -1) return if (sb.isEmpty()) null else sb.toString()
            if (c == '\n'.code) return sb.toString()
            sb.append(c.toChar())
            if (sb.length > MAX_LINE) throw java.io.IOException("Line too long")
        }
    }
}

/** One open link: blocking [read], thread-safe [send]. Closing it unblocks a pending read. */
class LinkConnection(private val socket: Socket) : Closeable {
    private val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
    private val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)

    init {
        socket.soTimeout = LinkProtocol.TIMEOUT_MS
        socket.tcpNoDelay = true
    }

    val remoteAddress: String get() = socket.inetAddress?.hostAddress.orEmpty()

    /** How long [read] waits before giving up on the other side. */
    fun timeout(ms: Int) {
        socket.soTimeout = ms
    }

    /** The next message; null when the other side closed. Unknown messages are skipped. */
    fun read(): Msg? {
        while (true) {
            val line = LinkProtocol.readLine(reader) ?: return null
            if (line.isBlank()) continue
            Msg.decode(line)?.let { return it }
        }
    }

    @Synchronized
    fun send(msg: Msg) {
        writer.write(Msg.encode(msg))
        writer.write("\n")
        writer.flush()
    }

    override fun close() {
        runCatching { socket.close() }
    }
}
