package net.freifunk.darmstadt.nodewhisperer.models.informations

class SystemLoad {
    val load: Double

    constructor(load: Double) {
        this.load = load
    }

    constructor(load: ByteArray) {
        /* Byte contains system load multiplied by 10 */
        this.load = load[0].toDouble() / 10.0
    }

    override fun toString(): String {
        return load.toString()
    }
}