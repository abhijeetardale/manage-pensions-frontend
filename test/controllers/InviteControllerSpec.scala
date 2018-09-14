/*
 * Copyright 2018 HM Revenue & Customs
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

import base.SpecBase
import connectors.SubscriptionConnector
import controllers.actions.{FakeUnAuthorisedAction, FakeAuthAction}
import models._
import org.scalatest.BeforeAndAfter
import org.scalatest.mockito.MockitoSugar
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, when, verify, times}
import play.api.test.Helpers._
import utils.MockDataHelper

import scala.concurrent.Future

class InviteControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfter{

  import InviteControllerSpec._

  val mockConnector = mock[SubscriptionConnector]
  val controller = new InviteController(mockAuthAction, mockConnector)

  before(reset(mockConnector))

  "InviteController calling onPageLoad" must {

    "return 200 if PSASuspension is false" in {

      when(mockConnector.getSubscriptionDetails(any())(any(), any())).thenReturn(Future.successful(
        SubscriptionDetails(psaSubscription)))

      val result = controller.onPageLoad(fakeRequest)

      status(result) mustBe OK
      verify(mockConnector,  times(1)).getSubscriptionDetails(any())(any(), any())

    }

    "return 303 if PSASuspension is true" in {

      when(mockConnector.getSubscriptionDetails(any())(any(), any())).thenReturn(Future.successful(
        SubscriptionDetails(psaSubscription.copy(isPSASuspension = true))))

      val result = controller.onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.YouCannotSendAnInviteController.onPageLoad().url)
      verify(mockConnector,  times(1)).getSubscriptionDetails(any())(any(), any())

    }

    "return 303 if request is unauthorised" in {

      val controller = new InviteController(FakeUnAuthorisedAction(), mockConnector)
      val result = controller.onPageLoad(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
      verify(mockConnector,  times(0)).getSubscriptionDetails(any())(any(), any())

    }
  }
}

object InviteControllerSpec extends MockDataHelper {
  private val customerIdentificationDetails = CustomerIdentificationDetails(legalStatus="AA", None, None, noIdentifier=false)
  private val declarationDetails = PensionSchemeAdministratorDeclaration(true, true, true, true, Some(true), Some(true), true, None)

  private val psaSubscription = PsaSubscriptionDetails(isPSASuspension=false,
    customerIdentificationDetails=customerIdentificationDetails,
    organisationOrPartnerDetails=None,
    individualDetails=None,
    correspondenceAddressDetails= address,
    correspondenceContactDetails = contactDetails,
    previousAddressDetails= indEstPrevAdd,
    numberOfDirectorsOrPartnersDetails=None,
    directorOrPartnerDetails=None,
    declarationDetails = declarationDetails)

  private val mockAuthAction =  FakeAuthAction()

}
