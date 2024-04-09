package net.freifunk.darmstadt.nodewhisperer.services

import net.freifunk.darmstadt.nodewhisperer.models.WifiScanResult

interface WifiScanServiceResultReceiver {
    fun onScanResultUpdate(wifiScanResults: List<WifiScanResult>)
}