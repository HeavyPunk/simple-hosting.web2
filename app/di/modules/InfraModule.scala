package di.modules

import com.google.inject.AbstractModule
import com.google.gson.GsonBuilder
import com.google.gson.Gson
import components.services.serializer.{JsonService, GsonJsonService}
import infra.gson.{OptionStringDeserializer, OptionIntDeserializer}
import infra.gson.OptionStringDeserializer

class InfraModule extends AbstractModule{
    override def configure(): Unit = {
        val gsonizer = new GsonBuilder()
            .registerTypeAdapter(classOf[Option[String]], new OptionStringDeserializer)
            .registerTypeAdapter(classOf[Option[Int]], new OptionIntDeserializer)
            .create()
        bind(classOf[Gson]).toInstance(gsonizer)
        bind(classOf[JsonService]).to(classOf[GsonJsonService])
    }
}
