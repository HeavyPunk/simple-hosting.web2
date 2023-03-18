package components.clients.curseforge.models

import com.google.gson.annotations.SerializedName
import components.clients.curseforge.models.business.Mod

class GetModResponse (
    @SerializedName("data") val data: Mod
)
