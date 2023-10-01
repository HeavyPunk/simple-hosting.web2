package components.clients.curseforge.clients.files

import components.clients.curseforge.models.GetFileDownloadUrlsResponse
import components.clients.curseforge.models.GetModFileByFileIdRequest
import components.clients.curseforge.models.GetModFileByFileIdResponse
import components.clients.curseforge.models.GetModFilesByModIdRequest
import components.clients.curseforge.models.GetModFilesByModIdResponse
import components.basic.Monad

trait CurseForgeFilesClient {
  def getModFilesByModId(request: GetModFilesByModIdRequest): Monad[Exception, GetModFilesByModIdResponse]
  def getModFileByFileId(request: GetModFileByFileIdRequest): Monad[Exception, GetModFileByFileIdResponse]
  def getFileDownloadUrls(request: GetModFilesByModIdRequest): Monad[Exception, GetFileDownloadUrlsResponse]
}
