package net.freifunk.darmstadt.nodewhisperer.models

data class CommunityInformation(
    val siteCode: String,
    val name: String,
    val shortName: String,
    val domainNames: Map<String, String>?
)
