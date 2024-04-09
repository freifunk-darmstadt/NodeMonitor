package net.freifunk.darmstadt.nodewhisperer.models.informations

class SystemUptime {
    var uptime: Long

    constructor(uptime: Long) {
        this.uptime = uptime
    }

    constructor(uptime: ByteArray) {
        /* Convert byte array (network byte order) to long */
        this.uptime = uptime.fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xff) }
    }

    override fun toString(): String {
        /* Format Time to human readable format */
        val sb = StringBuilder()
        val minutes = uptime % 60
        val hours = uptime / 60 % 24
        val days = uptime / 60 / 24

        if (days > 0) {
            sb.append(days).append("d ")
        }
        if (hours > 0) {
            sb.append(hours).append("h ")
        }
        sb.append(minutes).append("m ")

        return sb.toString()
    }
}