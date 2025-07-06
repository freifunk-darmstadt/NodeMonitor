@file:OptIn(ExperimentalMaterial3Api::class)

package net.freifunk.darmstadt.nodewhisperer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import net.freifunk.darmstadt.nodewhisperer.models.GluonNode
import net.freifunk.darmstadt.nodewhisperer.models.ScanResultListModel
import net.freifunk.darmstadt.nodewhisperer.models.WifiScanResult
import net.freifunk.darmstadt.nodewhisperer.models.enums.NodeStatus
import net.freifunk.darmstadt.nodewhisperer.services.CommunityService
import net.freifunk.darmstadt.nodewhisperer.services.NodeStatusService
import net.freifunk.darmstadt.nodewhisperer.services.WifiScanService
import net.freifunk.darmstadt.nodewhisperer.services.WifiScanServiceResultReceiver
import net.freifunk.darmstadt.nodewhisperer.ui.theme.NodeWhispererTheme
import org.json.JSONException
import java.io.FileNotFoundException
import java.lang.StringBuilder


class MainActivity : ComponentActivity() {
    val scanResultListModel = ScanResultListModel()
    val wifiScanService = WifiScanService(this)
    val communityService = CommunityService(this)

    fun haveAllPermissions(): Boolean {
        return checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.CHANGE_WIFI_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.ACCESS_WIFI_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun permissionToast() {
        Toast.makeText(this, getString(R.string.msg_permission_request), Toast.LENGTH_SHORT).show()
    }
    fun generateRawDebugInfo() =
        "scanning_enabled=${wifiScanService.scanningEnabled.value},\n" +
                "scanning_paused=${wifiScanService.scanningPaused.value},\n" +
                "total_nodes=${scanResultListModel.scanResults.size},\n" +
                "nodes=" + scanResultListModel.scanResults.joinToString(";\n") { node ->
            "hostname=${node.hostname ?: ""},\n" +
                    "node_id=${node.nodeId},\n" +
                    "status=${NodeStatusService.getNodeStatus(node)},\n" +
                    "site_code=${getSiteDomainString(node) ?: ""},\n" +
                    "last_seen=${node.lastSeen ?: ""},\n" +
                    "system_uptime=${node.systemUptime ?: ""},\n" +
                    "system_load=${node.systemLoad ?: ""},\n" +
                    "firmware_version=${node.firmwareVersion ?: ""},\n" +
                    "vpn_connected=${node.batmanAdv?.vpnConnected ?: ""},\n" +
                    "gateway_tq=${node.batmanAdv?.tq ?: ""},\n" +
                    "neighbors=${node.batmanAdv?.neighbors ?: ""},\n" +
                    "originators=${node.batmanAdv?.originators ?: ""},\n" +
                    "community_short_name=${node.communityInformation?.shortName ?: ""}"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            activityDesign(this, wifiScanService, scanResultListModel)
        }

        /* Check if WiFi Permission is granted */
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            /* Do nothing. We have a Task for that */
        }

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (checkSelfPermission(android.Manifest.permission.CHANGE_WIFI_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.CHANGE_WIFI_STATE)
        }

        if (checkSelfPermission(android.Manifest.permission.ACCESS_WIFI_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_WIFI_STATE)
        }

        wifiScanService.registerReceiver(object : WifiScanServiceResultReceiver {
            override fun onScanResultUpdate(wifiScanResults: List<WifiScanResult>) {
                Log.d("MainActivity", "Scan results updated {${wifiScanResults.size}}")
                updateScanResultList(wifiScanResults)
            }
        })

        if (!haveAllPermissions()) {
            permissionToast()
        } else {
            toggleScanState(wifiScanService, scanResultListModel)
        }
    }

    override fun onPause() {
        super.onPause()
        if (wifiScanService.scanningEnabled.value) {
            wifiScanService.pauseScanning()
        }
    }

    override fun onResume() {
        super.onResume()
        if (haveAllPermissions() && wifiScanService.scanningEnabled.value && wifiScanService.scanningPaused.value) {
            wifiScanService.resumeScanning()
        }
    }

    private fun updateScanResultList(wifiScanResults: List<WifiScanResult>) {
        for (node in scanResultListModel.scanResults) {
            if (node.lastSeen?.time!! < System.currentTimeMillis() - 60000) {
                scanResultListModel.scanResults.remove(node)
            }
        }

        /* Loop through new scan results */
        for (result in wifiScanResults) {
            if (result.getGluonElements().size == 0) {
                /* No Gluon Node VIF */
                continue
            }

            val node = GluonNode()

            try {
                node.addScanResult(result)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error adding scan result: ${e.message}")
                continue
            }

            if (node.nodeId == null || node.hostname == null)
                continue

            /* Find Node with matching NodeID in results */
            val existingNode = scanResultListModel.scanResults.find {
                it.nodeId?.macAddress.contentEquals(node.nodeId?.macAddress)
            }

            /* Add new Gluon node if not found, otherwise add scan result to existing node */
            if (existingNode == null) {
                scanResultListModel.scanResults.add(node)
            } else {
                existingNode.addScanResult(result)
            }
        }

        /* Add CommuityInformation */
        for (node in scanResultListModel.scanResults) {
            try {
                if (node.siteCode != null) {
                    node.communityInformation =
                        communityService.getCommunityInformation(node.siteCode!!.unresolved)

                    /* Update SiteCode */
                    node.siteCode!!.resolved = node.communityInformation!!.shortName

                    /* Update DomainCode */
                    if (node.domain != null &&
                        node.communityInformation!!.domainNames != null &&
                        node.communityInformation!!.domainNames!!.containsKey(node.domain!!.unresolved)
                    ) {
                        node.domain!!.resolved =
                            node.communityInformation!!.domainNames!![node.domain!!.unresolved]!!
                    }
                }
            } catch (e: FileNotFoundException) {
                Log.e("MainActivity", "Community file not found: ${e.message}")
                continue
            } catch (e: JSONException) {
                Log.e("MainActivity", "Community file not found: ${e.message}")
                continue
            }
        }
    }
}

fun toggleScanState(wifiScanService: WifiScanService, scanResultsList: ScanResultListModel) {
    if (wifiScanService.scanningEnabled.value) {
        wifiScanService.stopScanning()
        return
    }

    wifiScanService.startScanning()
}

fun getSiteDomainString(node: GluonNode): String? {
    if (node.siteCode == null)
        return null

    var sb = StringBuilder()

    if (node.siteCode != null) {
        if (node.siteCode!!.resolved != null) {
            sb.append(node.siteCode!!.resolved)
        } else {
            sb.append(node.siteCode!!.unresolved)
        }
    }

    if (node.domain != null) {
        sb.append(" (")
        if (node.domain!!.resolved != null) {
            sb.append(node.domain!!.resolved)
        } else {
            sb.append(node.domain!!.unresolved)
        }
        sb.append(")")
    }

    return sb.toString()
}

@Composable
fun getColorForNodeStatus(node: GluonNode): Color {
    return when (NodeStatusService.getNodeStatus(node)) {
        NodeStatus.OK -> colorResource(R.color.green_500)
        NodeStatus.MESH_ONLY -> colorResource(R.color.yellow_500)
        else -> colorResource(R.color.red_500)
    }
}

@Composable
fun getIconForNodeStatus(node: GluonNode): ImageVector {
    return when (NodeStatusService.getNodeStatus(node)) {
        NodeStatus.OK -> Icons.Default.Done
        NodeStatus.MESH_ONLY -> Icons.Default.Warning
        else -> Icons.Default.Close
    }
}

@Composable
fun activityDesign(
    activity: MainActivity,
    wifiScanService: WifiScanService? = null,
    scanResultsList: ScanResultListModel = ScanResultListModel()
) {
    NodeWhispererTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        colors = topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        title = {
                            Text("Knoten")
                        },
                        actions = {
                            IconButton(onClick = {
                                /* share debug info via share function */
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, activity.generateRawDebugInfo())
                                    type = "text/plain"
                                }
                                val launchBrowser = Intent.createChooser(sendIntent, null)
                                activity.startActivity(launchBrowser)
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share"
                                )
                            }
                            IconButton(onClick = {
                                /* Open darmstadt.freifunk.net */
                                val uriUrl = Uri.parse(activity.getString(R.string.url_help))
                                val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
                                activity.startActivity(launchBrowser)
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "Info"
                                )
                            }
                        },

                        )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (activity.haveAllPermissions()) {
                                toggleScanState(wifiScanService!!, scanResultsList)
                            } else if (wifiScanService != null && !wifiScanService.scanningEnabled.value) {
                                activity.permissionToast()
                            }
                        },
                        containerColor =
                        if (wifiScanService!!.scanningEnabled.value)
                            colorResource(R.color.red_500)
                        else
                            colorResource(R.color.green_500)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.wifi_find),
                            contentDescription = "Add",
                            Modifier.background(Color.Transparent)
                        )
                        Text(
                            text =
                            if (wifiScanService!!.scanningEnabled.value)
                                stringResource(R.string.fab_scan_stop)
                            else
                                stringResource(R.string.fab_scan_start),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),

                ) {
                    scanResultsList.scanResults.let {
                        itemsIndexed(scanResultsList.scanResults) { index, item ->
                            ScanResultListElement(
                                node = item
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NodeInfoBottomSheet(
    node: GluonNode,
    showBottomSheet: MutableState<Boolean>,
    scope: CoroutineScope,
    sheetState: SheetState
) {
    ModalBottomSheet(
        onDismissRequest = {
            showBottomSheet.value = false
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Icon(
                imageVector = getIconForNodeStatus(node),
                tint = getColorForNodeStatus(node),
                contentDescription = "Status",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(48.dp)
            )
            Text(
                text =
                    if (NodeStatusService.getNodeStatus(node) == NodeStatus.OK)
                        stringResource(R.string.node_status_okay)
                    else if (NodeStatusService.getNodeStatus(node) == NodeStatus.MESH_ONLY)
                        stringResource(R.string.node_status_mesh_only)
                    else
                        stringResource(R.string.node_status_error),
                textAlign = TextAlign.Center,
                fontSize = 24.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            if (NodeStatusService.getNodeStatus(node) == NodeStatus.MESH_ONLY) {
                Text(
                    text = stringResource(R.string.node_status_mesh_only_description),
                    textAlign = TextAlign.Left,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }

            Text(
                text = stringResource(R.string.node_title_information),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(4.dp)
            )
            NodeInfoDialogProperty(stringResource(R.string.node_name),
                node.hostname ?: stringResource(R.string.str_unknown)
            )
            NodeInfoDialogProperty(stringResource(R.string.node_id),
                node.nodeId.toString()
            )
            if (node.lastSeen != null)
                NodeInfoDialogProperty(stringResource(R.string.node_last_seen),
                    node.lastSeen.toString()
                )
            if (node.systemUptime != null)
                NodeInfoDialogProperty(stringResource(R.string.node_runtime),
                    node.systemUptime.toString()
                )
            if (getSiteDomainString(node) != null)
                NodeInfoDialogProperty(
                    stringResource(R.string.node_community),
                    getSiteDomainString(node)!!
                )
            if (node.systemLoad != null)
                NodeInfoDialogProperty(stringResource(R.string.node_system_load),
                    node.systemLoad.toString()
                )
            if (node.firmwareVersion != null)
                NodeInfoDialogProperty(stringResource(R.string.node_firmware_version),
                    node.firmwareVersion ?: stringResource(R.string.str_unknown)
                )
            if (node.batmanAdv != null) {
                NodeInfoDialogProperty(stringResource(R.string.node_vpn),
                    if (node.batmanAdv!!.vpnConnected) stringResource(R.string.node_vpn_connected) else stringResource(
                        R.string.node_vpn_not_connected
                    )
                )
                NodeInfoDialogProperty(stringResource(R.string.node_gateway),
                    if (node.batmanAdv!!.tq > 0) stringResource(
                        R.string.node_batman_gw_reachable,
                        (node.batmanAdv!!.tq / 255.0 * 100.0).toInt()
                    ) else stringResource(R.string.node_batman_gw_not_reachable)
                )
                NodeInfoDialogProperty(stringResource(R.string.node_neighbors),
                    node.batmanAdv!!.neighbors.toString()
                )
                NodeInfoDialogProperty(stringResource(R.string.node_nodes_in_network),
                    node.batmanAdv!!.originators.toString()
                )
            }
        }
    }
}

@Composable
fun NodeInfoDialogProperty(name: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = name,
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = content,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
fun ScanResultListElement(node: GluonNode) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clickable {
                showBottomSheet.value = true
            }
            .padding(8.dp))
    {
        Row (
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = node.hostname ?: stringResource(R.string.global_unknown),
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = node.nodeId.toString(),
                    textAlign = TextAlign.Start,
                )
            }
            Icon(
                imageVector = getIconForNodeStatus(node),
                tint = getColorForNodeStatus(node),
                contentDescription = "Add",
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(8.dp)
            )
        }
    }
    if (showBottomSheet.value) {
        NodeInfoBottomSheet(node, showBottomSheet, scope, sheetState)
    }
}