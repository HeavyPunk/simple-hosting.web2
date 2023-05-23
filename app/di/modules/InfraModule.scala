package di.modules

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.inject.AbstractModule
import components.services.serializer.JacksonJsonService
import components.services.serializer.JsonService
import com.fasterxml.jackson.databind.SerializationFeature
import components.services.log.Log
import play.api.Logger
import components.services.log.PlayFrameworkLog

class InfraModule extends AbstractModule {
    override def configure(): Unit = {
        val jsonMapper = JsonMapper.builder()
            .addModule(DefaultScalaModule)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .build()
        val logger = Logger("")

        bind(classOf[JsonMapper]).toInstance(jsonMapper)
        
        bind(classOf[JsonService]).to(classOf[JacksonJsonService])
        bind(classOf[Log]).toInstance(new PlayFrameworkLog(logger))
    }
}
