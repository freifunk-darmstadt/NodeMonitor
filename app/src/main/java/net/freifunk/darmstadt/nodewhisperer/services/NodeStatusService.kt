package net.freifunk.darmstadt.nodewhisperer.services

import net.freifunk.darmstadt.nodewhisperer.models.GluonNode
import net.freifunk.darmstadt.nodewhisperer.models.enums.NodeError
import net.freifunk.darmstadt.nodewhisperer.models.enums.NodeStatus

class NodeStatusService {
    companion object {
        fun getNodeErrors(gluonNode: GluonNode): List<NodeError> {
            val errors = mutableListOf<NodeError>()
            if (gluonNode.batmanAdv != null) {
                if (!gluonNode.batmanAdv!!.vpnConnected) {
                    errors.add(NodeError.NO_VPN_CONNECTED)
                }
                if (gluonNode.batmanAdv!!.tq == 0) {
                    errors.add(NodeError.NO_GATEWAY)
                }
                if (gluonNode.batmanAdv!!.originators == 0) {
                    errors.add(NodeError.NO_NODES_IN_NETWORK)
                }
            }
            return errors
        }
        fun getNodeStatus(gluonNode: GluonNode): NodeStatus {
            val errors = getNodeErrors(gluonNode)
            if (errors.contains(NodeError.NO_GATEWAY)) {
                return NodeStatus.CRITICAL
            }

            if (errors.contains(NodeError.NO_NODES_IN_NETWORK)) {
                return NodeStatus.CRITICAL
            }

            return NodeStatus.OK
        }
    }
}