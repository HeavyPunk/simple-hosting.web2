package di.modules

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.inject.AbstractModule
import components.services.serializer.JacksonJsonService
import components.services.serializer.JsonService

class InfraModule extends AbstractModule {
    override def configure(): Unit = {
        val jsonMapper = JsonMapper.builder()
            .addModule(DefaultScalaModule)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build()
        bind(classOf[JsonMapper]).toInstance(jsonMapper)
        
        bind(classOf[JsonService]).to(classOf[JacksonJsonService])
    }
}
