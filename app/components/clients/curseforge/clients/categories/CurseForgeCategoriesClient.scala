package components.clients.curseforge.clients.categories

import components.clients.curseforge.models.GetCategoriesGroupedByClassResponse
import components.clients.curseforge.models.GetCategoriesRequest
import components.clients.curseforge.models.GetCategoriesResponse

trait CurseForgeCategoriesClient {
  def getCategories(request: GetCategoriesRequest): GetCategoriesResponse
  def getCategoriesGroupedByClass(): GetCategoriesGroupedByClassResponse
}
