package components.clients.curseforge

import components.clients.curseforge.clients.categories.CurseForgeCategoriesClient
import components.clients.curseforge.clients.mods.CurseForgeModsClient
import components.clients.curseforge.clients.softwares.CurseForgeSoftwaresClient
import components.clients.curseforge.clients.files.CurseForgeFilesClient

import javax.inject.Inject

class CommonCurseForgeClient @Inject() (
    modsClient: CurseForgeModsClient,
    softwaresClient: CurseForgeSoftwaresClient,
    categoriesClient: CurseForgeCategoriesClient,
    filesClient: CurseForgeFilesClient
) extends CurseForgeClient {
  val mods: CurseForgeModsClient             = modsClient
  val softwares: CurseForgeSoftwaresClient   = softwaresClient
  val categories: CurseForgeCategoriesClient = categoriesClient
  val files: CurseForgeFilesClient           = filesClient
}
