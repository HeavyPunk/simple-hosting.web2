package components.clients.curseforge.clients.categories

import components.clients.curseforge.models.GetCategoriesGroupedByClassResponse
import components.clients.curseforge.models.GetCategoriesRequest
import components.clients.curseforge.models.GetCategoriesResponse
import components.basic.Monad

trait CurseForgeCategoriesClient {
  def getCategories(request: GetCategoriesRequest): Monad[Exception, GetCategoriesResponse]
  def getCategoriesGroupedByClass(): Monad[Exception, GetCategoriesGroupedByClassResponse]
}
