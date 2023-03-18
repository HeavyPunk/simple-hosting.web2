package di.modules

import com.google.inject.AbstractModule
import com.google.gson.GsonBuilder
import com.google.gson.Gson
import components.services.serializer.{JsonService, GsonJsonService}
import infra.gson.{OptionStringDeserializer, OptionIntDeserializer}
import infra.gson.OptionStringDeserializer
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import components.services.serializer.JacksonJsonService

class InfraModule extends AbstractModule{
    override def configure(): Unit = {
        val gsonizer = new GsonBuilder()
            .registerTypeAdapter(classOf[Option[Any]], new OptionIntDeserializer[Option[Any]])
            .create()
        bind(classOf[Gson]).toInstance(gsonizer)

        val jsonMapper = JsonMapper.builder()
            .addModule(DefaultScalaModule)
            .build()
        bind(classOf[JsonMapper]).toInstance(jsonMapper)
        
        bind(classOf[JsonService]).to(classOf[JacksonJsonService])
    }
}
