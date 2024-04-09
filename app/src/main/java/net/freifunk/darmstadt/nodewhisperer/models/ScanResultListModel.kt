package net.freifunk.darmstadt.nodewhisperer.models

import androidx.compose.runtime.snapshots.SnapshotStateList

class ScanResultListModel {
    val scanResults = SnapshotStateList<GluonNode>()
}