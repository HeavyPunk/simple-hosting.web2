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

  override def getCategories(request: GetCategoriesRequest): GetCategoriesResponse = {
    val query = request.toQueryString()
    val uri = constructBaseUri()
      .setCustomQuery(query)
      .appendPathSegments(ApiPaths.categories)
      .build()
    val req      = baseRequest.GET().uri(uri).build()
    val client   = HttpClient.newHttpClient()
    val response = client.send(req, BodyHandlers.ofString())

    val res = jsonizer.deserialize(response.body(), classOf[GetCategoriesResponse])
    res
  }

  override def getCategoriesGroupedByClass(): GetCategoriesGroupedByClassResponse = {
    val categories    = this.getCategories(new GetCategoriesRequest(432, null, null))
    val categoriesMap = collection.mutable.Map[String, GroupedCategory]()
    val classIds      = List[Int](5, 6, 12, 17, 4471).toArray

    for (classId <- classIds) {
      val mainClass       = categories.data.find(c => c.id == classId.toString).get
      val classCategories = categories.data.filter(c => c.classId == classId && c.classId == c.parentCategoryId)

      categoriesMap(classId.toString) = new GroupedCategory(mainClass.name, classCategories.sortBy(_.name.toLowerCase))
    }

    val formattedCategories = categoriesMap.values.toArray
    val res                 = new GetCategoriesGroupedByClassResponse(formattedCategories)
    res
  }
}
