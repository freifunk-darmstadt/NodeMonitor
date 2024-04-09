package net.freifunk.darmstadt.nodewhisperer.models.informations

class BatmanAdv {
    val vpnConnected: Boolean
    val tq: Int
    val originators: Int
    val neighbors: Int
    val clients: Int

    constructor(vpnConnected: Boolean, tq: Int, originators: Int, neighbors: Int, clients: Int) {
        this.vpnConnected = vpnConnected
        this.tq = tq
        this.originators = originators
        this.neighbors = neighbors
        this.clients = clients
    }

    constructor(batmanAdv: ByteArray) {
        vpnConnected = batmanAdv[0] == 1.toByte()

        /* Byte 1 is the TQ */
        val tq1 = batmanAdv[1].toUByte().toUInt()
        tq = tq1.toInt()

        /* Byte 2 and 3 are the number of originators (Big Endian) */
        originators = (batmanAdv[2].toInt() shl 8) or (batmanAdv[3].toInt() and 0xff)

        /* Byte 4 and 5 are the number of neighbors (Big Endian) */
        neighbors = (batmanAdv[4].toInt() shl 8) or (batmanAdv[5].toInt() and 0xff)

        /* Byte 6 and 7 are the number of clients (Big Endian) */
        clients = (batmanAdv[6].toInt() shl 8) or (batmanAdv[7].toInt() and 0xff)
    }
}