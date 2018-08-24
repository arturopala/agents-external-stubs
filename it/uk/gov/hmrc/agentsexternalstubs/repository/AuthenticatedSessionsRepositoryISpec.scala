/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.hmrc.agentsexternalstubs.repository

import java.util.UUID

import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.agentsexternalstubs.support.{MongoDbPerSuite, MongoDbPerTest}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class AuthenticatedSessionsRepositoryISpec extends UnitSpec with OneAppPerSuite with MongoDbPerTest {

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri"   -> mongoUri,
        "proxies.start" -> "false"
      )

  override implicit lazy val app: Application = appBuilder.build()

  lazy val repo: AuthenticatedSessionsRepository = app.injector.instanceOf[AuthenticatedSessionsRepository]

  "store" should {
    "store a session" in {
      val authToken = UUID.randomUUID().toString
      await(repo.create("foobar", authToken, "bla", "juniper"))

      val result = await(repo.find())

      result.size shouldBe 1
      result.head.authToken shouldBe authToken
      result.head.userId shouldBe "foobar"

    }

    "not allow duplicate sessions to be created for the same authToken" in {
      await(repo.create("foo", "bar", "bla", "juniper"))

      val e = intercept[DatabaseException] {
        await(repo.create("foo", "bar", "ala", "juniper"))
      }

      e.getMessage() should include("E11000")
    }
  }
}
