package components.services.retrier

import java.time.Duration
import scala.concurrent.Future

object  Retrier {
    def work[TRes](
        action: => TRes,
        isSuccess: TRes => Boolean,
        attempts: Int = 5,
        delay: Duration = Duration.ofSeconds(5),
        onException: Exception => Unit = e => {},
        onAttempt: Int => Unit = a => {},
    ): Option[TRes] = {
        for (i <- Seq.range(0, attempts)) {
            onAttempt(i)
            val res = try {
                Some(action)
            } catch {
                case e: Exception => onException(e); None
            }
            if (res.isDefined && isSuccess(res.get))
                return res
            Thread.sleep(delay.getSeconds * 1000) //TODO: переделать на асинхронность
        }
        None
    }
}
