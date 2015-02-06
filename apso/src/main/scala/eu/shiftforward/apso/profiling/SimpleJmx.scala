package eu.shiftforward.apso.profiling

import java.net.ServerSocket

import com.j256.simplejmx.server.JmxServer
import com.typesafe.config.Config
import eu.shiftforward.apso.Logging
import eu.shiftforward.apso.config.Implicits._

import scala.util.{ Failure, Success, Try }

trait SimpleJmx extends Logging {
  def jmxConfig: Config

  private lazy val jmxHost = jmxConfig.getStringOption("host")
  private lazy val jmxPort = jmxConfig.getIntOption("port")

  /**
   * A handler that is called after the `JmxServer` is successfully started.
   * By default it sets the JVM's shutdown hook to stop the `JmxServer`.
   * @param jmx the `JmxServer`
   */
  def onJmxStart(jmx: JmxServer) { sys.addShutdownHook(jmx.stop()) }

  private def startJmx(port: Option[Int] = None) = {
    def randomPort = {
      val s = new ServerSocket(0)
      val p = s.getLocalPort
      s.close()
      p
    }

    val jmx = new JmxServer(port.getOrElse(randomPort))
    jmx.start()
    jmx
  }

  // start a properly configured JMX server. When behind a firewall, both ports `jmxPort` (the RMI
  // registry port) and `jmxPort + 1` (the RMI server port) need to be open. Connections are
  // established through port `jmxPort`.
  // In the event of a binding failure to port `jmxPort`, a retry is performed with a random port.
  Try(startJmx(jmxPort)).recover { case _ => startJmx() } match {
    case Success(jmx) =>
      log.info("Bound JMX on port {}", jmx.getServerPort)
      onJmxStart(jmx)

    case Failure(ex) =>
      log.warn("Could not start JMX server", ex)
  }
}
