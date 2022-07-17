package clients

import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.http.scaladsl.model.StatusCodes.OK
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.syntax._
import io.circe.generic.auto._
import tags.ApiCall

class AkkaHttpClientSpec
    extends TestKit(ActorSystem("AkkaHttpClientSpec"))
    with AsyncWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with FailFastCirceSupport {

  val client          = new AkkaHttpClient()(system)
  val wireMockServer  = new WireMockServer(wireMockConfig().dynamicPort())
  val applicationJson = "application/json"
  case class DummyObject(someId: Int, someString: String)

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMockServer.stop()
    TestKit.shutdownActorSystem(system)
  }

  "AkkaHttpClientSpec" when {
    "get" should {
      "return response with status" taggedAs ApiCall in {
        val mockResponse = aResponse().withStatus(OK.intValue)
        wireMockServer.stubFor(get(urlEqualTo("/get?id=1")).willReturn(mockResponse))
        val get_url      = s"${wireMockServer.baseUrl()}/get"

        val responseFuture = client.get[DummyObject](get_url, params = Map("id" -> "1"))
        responseFuture.flatMap { response =>
          response.status shouldBe OK.intValue
        }
      }

      "return response with body" taggedAs ApiCall in {
        val dummyObject: DummyObject = DummyObject(12345, "something as object content")
        val response                 = aResponse()
          .withStatus(OK.intValue)
          .withHeader("Content-Type", applicationJson)
          .withBody(dummyObject.asJson.noSpaces)
        wireMockServer.stubFor(get(urlEqualTo("/get")).willReturn(response))
        val get_url                  = s"${wireMockServer.baseUrl()}/get"

        val responseFuture = client.get[DummyObject](get_url)
        responseFuture.map { response =>
          response.body shouldBe SuccessResponseBody(dummyObject)
        }
      }

      "return response with headers" taggedAs ApiCall in {
        val response = aResponse()
          .withStatus(OK.intValue)
          .withHeader("Content-Type", applicationJson)
          .withHeader("TestKey", "TestValue")
        wireMockServer.stubFor(get(urlEqualTo("/get?id=1")).willReturn(response))
        val get_url  = s"${wireMockServer.baseUrl()}/get"

        client
          .get[DummyObject](get_url, params = Map("id" -> "1"))
          .map(_.headers.getOrElse("TestKey", List("failed")) shouldBe List("TestValue"))
      }
    }
  }
}
