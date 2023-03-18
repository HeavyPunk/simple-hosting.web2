package infra.gson

import com.google.gson.InstanceCreator
import java.lang.reflect.Type
import com.google.gson.JsonDeserializer
import com.google.gson.{JsonDeserializationContext, JsonElement}

class OptionStringDeserializer extends JsonDeserializer[Option[String]] {
    def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Option[String] = Option(json.getAsString())
}

class OptionIntDeserializer extends JsonDeserializer[Option[Int]] {
    def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Option[Int] = Option(json.getAsInt())
}
