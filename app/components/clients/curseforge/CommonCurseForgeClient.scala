package components.clients.curseforge

import components.clients.curseforge.clients.categories.CurseForgeCategoriesClient
import components.clients.curseforge.clients.mods.CurseForgeModsClient
import components.clients.curseforge.clients.softwares.CurseForgeSoftwaresClient

import javax.inject.Inject

class CommonCurseForgeClient @Inject() (
    modsClient: CurseForgeModsClient,
    softwaresClient: CurseForgeSoftwaresClient,
    categoriesClient: CurseForgeCategoriesClient
) extends CurseForgeClient {
  val mods: CurseForgeModsClient             = modsClient
  val softwares: CurseForgeSoftwaresClient   = softwaresClient
  val categories: CurseForgeCategoriesClient = categoriesClient
}
