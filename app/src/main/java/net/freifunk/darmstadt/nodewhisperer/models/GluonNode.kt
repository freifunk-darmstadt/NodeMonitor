package net.freifunk.darmstadt.nodewhisperer.models

import android.util.Log
import net.freifunk.darmstadt.nodewhisperer.models.informations.BatmanAdv
import net.freifunk.darmstadt.nodewhisperer.models.informations.NodeId
import net.freifunk.darmstadt.nodewhisperer.models.informations.SiteDomainCode
import net.freifunk.darmstadt.nodewhisperer.models.informations.SystemLoad
import net.freifunk.darmstadt.nodewhisperer.models.informations.SystemUptime
import java.util.Date

class GluonNode {
    var nodeId: NodeId? = null
    var systemUptime: SystemUptime? = null
    var batmanAdv: BatmanAdv? = null
    var scanResults: List<WifiScanResult> = emptyList()
    var lastSeen: Date? = Date()
    var hostname: String? = null
    var siteCode: SiteDomainCode? = null
    var domain: SiteDomainCode? = null
    var systemLoad: SystemLoad? = null
    var firmwareVersion: String? = null

    /* Additional Information */
    var communityInformation: CommunityInformation? = null

    fun addScanResult(scanResult: WifiScanResult) {
        if (scanResult.getGluonElements().size == 0) {
            Log.d("GluonNode", "No Gluon elements found in scan result")
            return
        }

        scanResults += scanResult

        /* Parse ScanResult */
        for (element in scanResult.getGluonElements()) {
            /* Dump ByteArray as hex string */
            val hexString = element.elementBytes.joinToString("") { "%02x".format(it) }
            Log.d("GluonNode", "type=${element.id} val=$hexString")

            try {
                when (element.id) {
                    0 -> {
                        hostname = String(element.elementBytes)
                    }
                    1 -> {
                        nodeId = NodeId(element.elementBytes)
                    }
                    2 -> {
                        systemUptime = SystemUptime(element.elementBytes)
                    }
                    3 -> {
                        siteCode = SiteDomainCode(String(element.elementBytes))
                    }
                    4 -> {
                        domain = SiteDomainCode(String(element.elementBytes))
                    }
                    5 -> {
                        systemLoad = SystemLoad(element.elementBytes)
                    }
                    6 -> {
                        firmwareVersion = String(element.elementBytes)
                    }
                    20 -> {
                        batmanAdv = BatmanAdv(element.elementBytes)
                    }
                }
            } catch (e: Exception) {
                Log.d("GluonNode", "Error parsing Gluon element id=${element.id} length=${element.elementBytes.size} data=${hexString} err=${e.message}")
            }
        }

        lastSeen = Date()
    }

    fun getNumberOfUniqueMacAddresses(): Int {
        val macAddresses = mutableSetOf<String>()
        for (scanResult in scanResults) {
            macAddresses.add(scanResult.bssid)
        }

        return macAddresses.size
    }
}