package components.clients.curseforge.clients.categories

import components.clients.curseforge.ApiPaths
import components.clients.curseforge.CurseForgeClientSettings
import components.clients.curseforge.models.GetCategoriesGroupedByClassResponse
import components.clients.curseforge.models.GetCategoriesRequest
import components.clients.curseforge.models.GetCategoriesResponse
import components.clients.curseforge.models.GroupedCategory
import components.services.serializer.JsonService
import org.apache.hc.core5.net.URIBuilder

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import javax.inject.Inject
import components.basic.{ Monad, ErrorMonad, ResultMonad, mapToMonad, zipWith }
import scala.collection.mutable
import scala.util.boundary, boundary.break

class CategoryNotFound
class CategoriesNotFound

class CommonCurseForgeCategoriesClient @Inject() (
    val settings: CurseForgeClientSettings,
    val jsonizer: JsonService
) extends CurseForgeCategoriesClient {
  var baseRequest = HttpRequest
    .newBuilder()
    .header("x-api-key", settings.apiKey)
    .header("Content-Type", "application/json")
    .header("Accept", "application/json")

  def constructBaseUri() = new URIBuilder()
    .setScheme("https")
    .setHost(settings.host)
    .setPathSegments("v1")

  override def getCategories(request: GetCategoriesRequest): Monad[Exception, GetCategoriesResponse] = {
    try {
      val query = request.toQueryString()
      val uri = constructBaseUri()
        .setCustomQuery(query)
        .appendPathSegments(ApiPaths.categories)
        .build()
      val req      = baseRequest.GET().uri(uri).build()
      val client   = HttpClient.newHttpClient()
      val response = client.send(req, BodyHandlers.ofString())

      val res = jsonizer.deserialize(response.body(), classOf[GetCategoriesResponse])
      ResultMonad(res)
    } catch {
      case e: Exception => ErrorMonad(e)
    }
  }

  override def getCategoriesGroupedByClass(): Monad[Exception, GetCategoriesGroupedByClassResponse] = boundary {
    val classIds = Array(5, 6, 12, 17, 4471)
    val categories = getCategories(new GetCategoriesRequest(432, null, null))
    val categoriesMap = mutable.Map[String, GroupedCategory]()
    for (classId <- classIds) {
      val mainClass = categories
        .flatMap(cats => cats.data.find(c => c.id == classId).mapToMonad(CategoryNotFound()))
      val classCategories = categories
        .flatMap(cats => cats.data.filter(c => c.classId == classId && c.classId == c.parentCategoryId).mapToMonad(CategoriesNotFound()))
      val executionResult = mainClass.zipWith(classCategories)
        .flatMap((mc, cc) => {
          categoriesMap(classId.toString) = GroupedCategory(mc.id, mc.name, cc.sortBy(_.name.toLowerCase))
          ResultMonad(true)
        })
      val error = executionResult.tryGetValue._1
      if (error != null){
        break(error match
          case _: CategoryNotFound => ResultMonad[Exception,GetCategoriesGroupedByClassResponse](GetCategoriesGroupedByClassResponse(Array()))
          case _: CategoriesNotFound => ResultMonad[Exception,GetCategoriesGroupedByClassResponse](GetCategoriesGroupedByClassResponse(Array()))
          case e: Exception => ErrorMonad[Exception,GetCategoriesGroupedByClassResponse](e))
      }
    }
    return ResultMonad(GetCategoriesGroupedByClassResponse(categoriesMap.values.toArray))
  }
}
