package components.services.log

import play.api.Logger

trait Log {
    def info(m: String): Unit
    def debug(m: String): Unit
    def warn(m: String): Unit
    def error(m: String): Unit
    def forContext(c: String): Log
}

class PlayFrameworkLog(logger: Logger) extends Log {

    override def forContext(c: String): Log = new PlayFrameworkLog(Logger(s"${logger.logger.getName} $c"))

    override def info(m: String): Unit = logger.info(m)

    override def debug(m: String): Unit = logger.debug(m)

    override def warn(m: String): Unit = logger.warn(m)

    override def error(m: String): Unit = logger.error(m)

}

class FakeLog extends Log {

  override def info(m: String): Unit = {}

  override def debug(m: String): Unit = {}

  override def warn(m: String): Unit = {}

  override def error(m: String): Unit = {}

  override def forContext(c: String): Log = this
}