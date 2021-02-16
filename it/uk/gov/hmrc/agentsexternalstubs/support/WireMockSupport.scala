package uk.gov.hmrc.agentsexternalstubs.support

import java.net.URL

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

case class WireMockBaseUrl(value: URL)

trait WireMockSupport extends BeforeAndAfterAll with BeforeAndAfterEach {
  me: Suite =>

  def commonStubs(): Unit = {}

  def wireMockPort: Int
  val wireMockHost = "localhost"
  def wireMockBaseUrlAsString = s"http://$wireMockHost:$wireMockPort"
  def wireMockBaseUrl = new URL(wireMockBaseUrlAsString)

  protected def basicWireMockConfig(): WireMockConfiguration = wireMockConfig()

  private lazy val wireMockServer = new WireMockServer(basicWireMockConfig().port(wireMockPort))

  override def beforeAll(): Unit = {
    super.beforeAll()
    WireMock.configureFor(wireMockHost, wireMockPort)
    wireMockServer.start()
  }

  override def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    WireMock.reset()
    commonStubs()
  }
}
