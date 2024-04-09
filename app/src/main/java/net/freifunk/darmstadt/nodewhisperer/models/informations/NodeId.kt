package net.freifunk.darmstadt.nodewhisperer.models.informations

class NodeId {
    var macAddress: ByteArray

    constructor(nodeId: String) {
        macAddress = nodeId.split(":").map { it.toInt(16).toByte() }.toByteArray()
    }

    constructor(macAddress: ByteArray) {
        if (macAddress.size != 6)
            throw IllegalArgumentException("macAddress must have a length of 6")
        this.macAddress = macAddress
    }

    override fun toString(): String {
        return macAddress.joinToString(":") {
            it.toUByte().toString(16).padStart(2, '0')
        }
    }
}