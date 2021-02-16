package uk.gov.hmrc.agentsexternalstubs.support
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.{Lock, ReentrantLock}

import com.codahale.metrics.{MetricRegistry, SharedMetricRegistries}
import com.kenshoo.play.metrics.{DisabledMetricsFilter, Metrics, MetricsFilter}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.TestServer
import uk.gov.hmrc.play.bootstrap.graphite.GraphiteMetricsModule
import uk.gov.hmrc.play.it.Port

import scala.concurrent.Await

trait TestPlayServer {

  private val testServer: AtomicReference[TestServer] = new AtomicReference[TestServer]()
  private val lock: Lock = new ReentrantLock()

  val port: Int = Port.randomAvailable
  val wireMockPort: Int = Port.randomAvailable

  lazy val app: Application = appBuilder.build()

  def configuration: Seq[(String, Any)] = Seq(
    "microservice.services.auth.port"                      -> Port.randomAvailable,
    "microservice.services.citizen-details.port"           -> Port.randomAvailable,
    "microservice.services.users-groups-search.port"       -> Port.randomAvailable,
    "microservice.services.enrolment-store-proxy.port"     -> Port.randomAvailable,
    "microservice.services.tax-enrolments.port"            -> Port.randomAvailable,
    "microservice.services.des.port"                       -> Port.randomAvailable,
    "microservice.services.ni-exemption-registration.port" -> Port.randomAvailable,
    "microservice.services.agent-access-control.port"      -> wireMockPort,
    "microservice.services.api-platform-test-user.port"    -> wireMockPort,
    "metrics.enabled"                                      -> false,
    "auditing.enabled"                                     -> false,
    "mongodb.uri"                                          -> MongoDB.uri,
    "http.port"                                            -> port
  )

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(configuration: _*)
      .disable[GraphiteMetricsModule]
      .overrides(bind[MetricsFilter].to[DisabledMetricsFilter])
      .overrides(bind[Metrics].to[TestMetrics])

  def run(): Unit =
    if (lock.tryLock()) try if (testServer.get() == null) {
      println(s"Starting TestPlayServer at $port ... ")
      val server = TestServer(port, app)
      server.start()
      val wsClient = app.injector.instanceOf[WSClient]
      import scala.concurrent.duration._
      Await.result(wsClient.url(s"http://localhost:$port/ping/ping").withRequestTimeout(5.seconds).get(), 5.seconds)
      testServer.set(server)
      Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
        override def run(): Unit =
          stop()
      }))
      println("ready.")
    } finally lock.unlock()

  def stop(): Unit =
    if (lock.tryLock()) try if (testServer.get() != null) {
      println(s"Stopping TestPlayServer at $port ...")
      testServer.get().stop()
      testServer.set(null)
    } finally lock.unlock()

}

object TestPlayServer extends TestPlayServer

class TestMetrics extends Metrics {

  def defaultRegistry: MetricRegistry = SharedMetricRegistries.getOrCreate("test")
  override def toJson: String = ""
}
