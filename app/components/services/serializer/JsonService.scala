package components.services.serializer

import com.google.gson.Gson
import javax.inject.Inject
import com.fasterxml.jackson.databind.json.JsonMapper


trait JsonService {
    def serialize[T](obj: T): String
    def deserialize[T](json: String, t: Class[T]): T
}

class GsonJsonService @Inject() (val json: Gson) extends JsonService {
    override def serialize[T](obj: T): String = json.toJson(obj)
    override def deserialize[T](data: String, t: Class[T]): T = json.fromJson(data, t)
}

class JacksonJsonService @Inject() (val json: JsonMapper) extends JsonService {

  override def serialize[T](obj: T): String = json.writeValueAsString(obj)

  override def deserialize[T](js: String, t: Class[T]): T = json.readValue(js, t)
}