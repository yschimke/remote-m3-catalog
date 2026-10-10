package ee.schimke.catalog.typed

import ee.schimke.composeai.discovery.UiBuilderPolicyFile

// Only the baseline tests read these paths. Generation calls the authored functions directly.
data class AuthoredPolicy(
  val outputName: String,
  val baseline: String,
  val policy: UiBuilderPolicyFile,
)

fun authoredPolicies(): List<AuthoredPolicy> =
  listOf(AuthoredPolicy("remote-m3", "remote-catalog/ui-builder.policy.json", remoteM3Policy()))
