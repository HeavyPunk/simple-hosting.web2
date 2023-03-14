package components.clients.curseforge.models

import components.clients.curseforge.models.business.Mod
import com.google.gson.annotations.SerializedName

class GetModsResponse (
    @SerializedName("data") val data: Array[Mod]
)
