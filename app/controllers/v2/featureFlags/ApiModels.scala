package controllers.v2.featureFlags

import com.fasterxml.jackson.annotation.JsonProperty

case class FeatureFlag(
    val name: String,
    val value: String,
    @JsonProperty("type") val _type: String,
)

case class GetFeatureFlagsResponse(
    val groups: Map[Long, Array[FeatureFlag]]
)
