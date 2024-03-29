package di.modules

import com.google.inject.AbstractModule
import components.clients.curseforge.CommonCurseForgeClient
import components.clients.curseforge.CurseForgeClient
import components.clients.curseforge.CurseForgeClientSettings
import components.clients.curseforge.clients.categories.CommonCurseForgeCategoriesClient
import components.clients.curseforge.clients.categories.CurseForgeCategoriesClient
import components.clients.curseforge.clients.mods.CommonCurseForgeModsClient
import components.clients.curseforge.clients.mods.CurseForgeModsClient
import components.clients.curseforge.clients.softwares.CommonCurseForgeSoftwaresClient
import components.clients.curseforge.clients.softwares.CurseForgeSoftwaresClient
import components.clients.curseforge.clients.files.CommonCurseForgeFilesClient
import components.clients.curseforge.clients.files.CurseForgeFilesClient
import io.github.heavypunk.compositor.client.{CompositorClient, CommonCompositorClient}
import io.github.heavypunk.compositor.client.settings.ClientSettings
import java.net.URI
import io.github.heavypunk.controller.client.ControllerClient
import io.github.heavypunk.controller.client.Settings
import io.github.heavypunk.controller.client.server.CommonControllerServerClient
import io.github.heavypunk.controller.client.server.ControllerServerClient
import io.github.heavypunk.controller.client.CommonControllerClient
import components.clients.controller.ControllerClientFactory
import play.api.Configuration
import play.api.Environment
import components.clients.compositor.CompositorClientWrapper
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import components.clients.juggernaut.JuggernautClient
import java.time.Duration

class ClientsModule(
  environment: Environment,
  configuration: Configuration,
) extends AbstractModule {
  override def configure() = {
    
    val curseforgeUri = configuration.get[String]("app.clients.curseforge.uri")
    val curseforgeApikey = configuration.get[String]("app.clients.curseforge.apikey")
    val curseForgeClientSettings = CurseForgeClientSettings(curseforgeUri, curseforgeApikey)

    bind(classOf[CurseForgeClientSettings]).toInstance(curseForgeClientSettings)
    bind(classOf[CurseForgeCategoriesClient]).to(classOf[CommonCurseForgeCategoriesClient])
    bind(classOf[CurseForgeSoftwaresClient]).to(classOf[CommonCurseForgeSoftwaresClient])
    bind(classOf[CurseForgeModsClient]).to(classOf[CommonCurseForgeModsClient])
    bind(classOf[CurseForgeFilesClient]).to(classOf[CommonCurseForgeFilesClient])
    bind(classOf[CurseForgeClient]).to(classOf[CommonCurseForgeClient])

    val compositorUri = configuration.get[String]("app.clients.compositor.uri")
    val compositorApikey = configuration.get[String]("app.clients.compositor.apikey")
    val compositorClientSettings = ClientSettings(new URI(compositorUri), compositorApikey)
    val compositorClient = new CommonCompositorClient(compositorClientSettings)
    bind(classOf[CompositorClient]).toInstance(compositorClient)

    val compositorClientWrapper = CompositorClientWrapper(compositorClient)
    bind(classOf[CompositorClientWrapper]).toInstance(compositorClientWrapper)

    //-----------------------------
    val controllerClientScheme = configuration.get[String]("app.clients.controller.scheme")
    val controllerClientHost = configuration.get[String]("app.clients.controller.host")
    val controllerClientPort = configuration.get[Int]("app.clients.controller.port")
    val controllerClientBaseSettings = new Settings("http", "127.0.0.1", 8989)
    bind(classOf[Settings]).toInstance(controllerClientBaseSettings)

    //-----------------------------
    val s3AccessKey = configuration.get[String]("app.clients.s3.accesskey")
    val s3SecretKey = configuration.get[String]("app.clients.s3.secretkey")
    val s3Endpoint = configuration.get[String]("app.clients.s3.endpoint")
    val awsCredentials = BasicAWSCredentials(s3AccessKey, s3SecretKey)
    val amazonS3Client = AmazonS3ClientBuilder
      .standard()
      .withCredentials(AWSStaticCredentialsProvider(awsCredentials))
      .withEndpointConfiguration(EndpointConfiguration(s3Endpoint, "us-east-1"))
      .withPathStyleAccessEnabled(true) //NOTE: без этой хуйни minio не робит 
      .build()
    bind(classOf[AmazonS3]).toInstance(amazonS3Client)

    //----------------------------
    val juggernautScheme = configuration.get[String]("app.clients.juggernaut.scheme")
    val juggernautHost = configuration.get[String]("app.clients.juggernaut.host")
    val juggernautPort = configuration.get[Int]("app.clients.juggernaut.port")
    bind(classOf[JuggernautClient]).toInstance(JuggernautClient(juggernautScheme, juggernautHost, juggernautPort, Duration.ofMinutes(2)))
  }
}
