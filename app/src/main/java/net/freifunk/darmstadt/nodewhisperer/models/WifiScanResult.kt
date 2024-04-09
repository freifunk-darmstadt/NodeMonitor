package net.freifunk.darmstadt.nodewhisperer.models

import android.net.wifi.ScanResult
import android.util.Log

class WifiScanResult {
    var ssid: String
    var bssid: String
    var informationElements: List<ScanResult.InformationElement> = listOf()
    var rssi: Int

    constructor(scanResult: ScanResult) {
        bssid = scanResult.BSSID
        ssid = scanResult.SSID
        informationElements = scanResult.informationElements
        rssi = scanResult.level
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun getGluonElements(): List<GluonElement> {
        var gluonElements = informationElements.filter {
            /* Filter Vendor Specific Elements */
            it.id == 221
        }.map {
            /* Map Vendor Specific Elements to ByteArray */
            val elementBytes = ByteArray(it.bytes.remaining())
            it.bytes.get(elementBytes)
            elementBytes
        }.filter {
            /* Filter for Gluon Elements */
            it.size >= 4 && it[0] == 0x00.toByte() && it[1] == 0x20.toByte() && it[2] == 0x91.toByte() && it[3] == 0x04.toByte()
        }.map {
            /* Remove the first 4 bytes */
            it.copyOfRange(4, it.size)
        }.toList()

        if (gluonElements.size > 1) {
            Log.w("WifiScanResult", "Multiple Gluon Elements found in one ScanResult")
            return emptyList()
        }

        val outputElements = mutableListOf<GluonElement>()
        gluonElements.forEach() {
            /* Parse TLV Values */
            var tmp = it
            while (true) {
                if (tmp.size < 2) {
                    break
                }

                val type = tmp[0].toInt()
                val length = tmp[1].toInt()

                if (tmp.size < 2 + length) {
                    break
                }

                val value = tmp.copyOfRange(2, 2 + length)
                tmp = tmp.copyOfRange(2 + length, tmp.size)

                outputElements.add(GluonElement(type, value))
            }
        }

        return outputElements
    }
}