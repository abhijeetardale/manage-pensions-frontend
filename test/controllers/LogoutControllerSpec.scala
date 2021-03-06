/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers

import connectors.aft.AftCacheConnector
import controllers.actions.FakeAuthAction
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class LogoutControllerSpec extends ControllerSpecBase with MockitoSugar {

  private val mockAftCacheConnector = mock[AftCacheConnector]

  private def logoutController = new LogoutController(frontendAppConfig, mockAftCacheConnector, FakeAuthAction(),
    stubMessagesControllerComponents())

  "Logout Controller" must {

    "redirect to feedback survey page for an Individual" in {

      when(mockAftCacheConnector.removeLock(any(), any())).thenReturn(Future.successful(Ok))
      val result = logoutController.onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(frontendAppConfig.serviceSignOut)
    }
  }
}
