package components.clients.juggernaut

import components.basic.Monad
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import akka.http.scaladsl.model.headers.CacheDirectives.`private`
import java.net.http.HttpRequest
import org.apache.hc.core5.net.URIBuilder
import java.time.Duration
import java.net.http.HttpClient
import java.net.http.HttpResponse.BodyHandlers
import components.basic.ErrorMonad
import components.basic.ResultMonad

class TaskNotFound

class JuggernautClient(scheme: String, host: String, port: Int, timeout: Duration) {
    val jsonizer = JsonMapper.builder()
        .addModule(DefaultScalaModule)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        .build()

    private def getBaseRequestBuilder() = HttpRequest.newBuilder()
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")

    private def constructBaseUri() = new URIBuilder()
        .setScheme(scheme)
        .setHost(host)
        .setPort(port)

    def createDownloadFileTask(request: DownloadFileTask): Monad[Exception, String] = {
        val ctx = jsonizer.writeValueAsString(request)
        val task = Task(
            kind = "download-file",
            context = ctx
        )
        sendTask(task, timeout)
    }

    def checkTaskStatus(taskId: String): Monad[Exception | TaskNotFound, String] = {
        val uri = constructBaseUri()
            .appendPath("task-status")
            .appendPath(taskId)
            .build()
        val req = getBaseRequestBuilder().GET()
            .uri(uri)
            .timeout(timeout)
            .build()

        val client = HttpClient.newHttpClient()
        val response = client.send(req, BodyHandlers.ofString())
        if (response.statusCode() >= 400)
            ErrorMonad(new RuntimeException(s"[JuggernautClient] failed to put task: code: ${response.statusCode()}"))
        else{
            val taskStatus = jsonizer.readValue(response.body(), classOf[TaskStatus])
            if taskStatus.found then ResultMonad(taskStatus.taskStatus) else ErrorMonad(TaskNotFound())
        }
    }

    private def sendTask(task: Task, timeout: Duration): Monad[Exception, String] = {
        val uri = constructBaseUri()
            .appendPath("task-put")
            .build()
        val content = jsonizer.writeValueAsString(task)
        val req = getBaseRequestBuilder().POST(HttpRequest.BodyPublishers.ofString(content))
            .uri(uri)
            .timeout(timeout)
            .build()

        val client = HttpClient.newHttpClient()
        val response = client.send(req, BodyHandlers.ofString())
        if (response.statusCode() >= 400)
            ErrorMonad(new RuntimeException(s"[JuggernautClient] failed to put task: code: ${response.statusCode()}"))
        else ResultMonad(jsonizer.readValue(response.body(), classOf[String]))
    }
}
