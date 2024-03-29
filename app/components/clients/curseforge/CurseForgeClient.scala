package components.clients.curseforge

import components.clients.curseforge.clients.categories.CurseForgeCategoriesClient
import components.clients.curseforge.clients.mods.CurseForgeModsClient
import components.clients.curseforge.clients.softwares.CurseForgeSoftwaresClient
import components.clients.curseforge.clients.files.CurseForgeFilesClient

trait CurseForgeClient {
  val mods: CurseForgeModsClient
  val softwares: CurseForgeSoftwaresClient
  val categories: CurseForgeCategoriesClient
  val files: CurseForgeFilesClient
}
