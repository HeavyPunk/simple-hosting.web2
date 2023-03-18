package infra.gson

import scala.reflect.runtime.universe._
import com.google.gson.InstanceCreator
import java.lang.reflect.Type
import com.google.gson.JsonDeserializer
import com.google.gson.{JsonDeserializationContext, JsonElement}
import java.lang.reflect.ParameterizedType

class OptionStringDeserializer extends JsonDeserializer[Option[String]] {
    def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Option[String] = Option(json.getAsString())
}

class OptionIntDeserializer[T] extends JsonDeserializer[Option[T]] {

    def getValidJsonDataFor[T: TypeTag](v: T, el: JsonElement): Option[Any] = typeOf[T] match {
        case t if t =:= typeOf[Option[String]] => Option(el.getAsString)
        case t if t =:= typeOf[Option[Int]] => Option(el.getAsInt)
        case _ => None
    }

    def getOptionType(t: Type, el: JsonElement):Option[Any] = {
        val optStr = classOf[Option[String]]
        val optInt = classOf[Option[Int]]
        t match {
            case optStr => Some(el.getAsString)
            case optInt => Some(el.getAsInt)
        }
    }

    def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Option[T] = {
        val t = typeOfT.asInstanceOf[ParameterizedType]
        val args = t.getActualTypeArguments
        val result = getOptionType(args(0), json)
        getValidJsonDataFor(typeOfT, json).asInstanceOf[Option[T]]
    }
}
