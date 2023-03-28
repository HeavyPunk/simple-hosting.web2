package components.clients.curseforge.clients.files

import components.clients.curseforge.models.GetFileDownloadUrlsResponse
import components.clients.curseforge.models.GetModFileByFileIdRequest
import components.clients.curseforge.models.GetModFileByFileIdResponse
import components.clients.curseforge.models.GetModFilesByModIdRequest
import components.clients.curseforge.models.GetModFilesByModIdResponse

trait CurseForgeFilesClient {
  def getModFilesByModId(request: GetModFilesByModIdRequest): GetModFilesByModIdResponse
  def getModFileByFileId(request: GetModFileByFileIdRequest): GetModFileByFileIdResponse
  def getFileDownloadUrls(request: GetModFilesByModIdRequest): GetFileDownloadUrlsResponse
}
