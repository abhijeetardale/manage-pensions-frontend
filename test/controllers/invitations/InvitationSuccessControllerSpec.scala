/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.invitations

import java.time.LocalDate

import config.FrontendAppConfig
import connectors.FakeUserAnswersCacheConnector
import controllers.actions._
import controllers.behaviours.ControllerWithNormalPageBehaviours
import models.MinimalSchemeDetail
import play.api.mvc.Call
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.{DateHelper, UserAnswers}
import views.html.invitations.invitation_success

class InvitationSuccessControllerSpec extends ControllerWithNormalPageBehaviours {

  private val testSrn: String = "test-srn"
  private val testInviteeName = "test-invitee-name"
  private val testPstr = "test-pstr"
  private val testSchemeName = "test-scheme-name"
  private val testSchemeDetail = MinimalSchemeDetail(testSrn, Some(testPstr), testSchemeName)
  private lazy val continue: Call = controllers.invitations.routes.InvitationSuccessController.onSubmit(testSrn)

  private val userAnswer = UserAnswers()
    .inviteeName(testInviteeName)
    .minimalSchemeDetails(testSchemeDetail)
    .dataRetrievalAction

  private val invitationSuccessView = injector.instanceOf[invitation_success]

  def viewAsString() = invitationSuccessView(
    testInviteeName,
    testSchemeName,
    testExpiryDate(frontendAppConfig),
    continue)(fakeRequest, messages).toString

  def onPageLoadAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new InvitationSuccessController(
      messagesApi, frontendAppConfig, fakeAuth, dataRetrievalAction, requiredDateAction, FakeUserAnswersCacheConnector, navigator,
      stubMessagesControllerComponents(), invitationSuccessView).onPageLoad(testSrn)
  }

  def onSubmitAction(dataRetrievalAction: DataRetrievalAction, fakeAuth: AuthAction) = {

    new InvitationSuccessController(
      messagesApi, frontendAppConfig, fakeAuth, dataRetrievalAction, requiredDateAction, FakeUserAnswersCacheConnector, navigator,
      stubMessagesControllerComponents(), invitationSuccessView).onSubmit(testSrn)
  }

  def testExpiryDate(config: FrontendAppConfig): LocalDate = {
    LocalDate.now().plusDays(config.invitationExpiryDays)
  }

  def redirectionCall(): Call = onwardRoute

  behave like controllerWithOnPageLoadMethod(onPageLoadAction, getEmptyData, Some(userAnswer), viewAsString)

  "InvitationSuccessController" when {
    "on PageLoad" must {
      "remove all the data from the cache" in {
        onPageLoadAction(userAnswer, FakeAuthAction())(fakeRequest)

        FakeUserAnswersCacheConnector.verifyAllDataRemoved
      }
    }
  }

  "calculate the correct invitation expiry date as today's date plus 30 days" in {

    val expected = DateHelper.formatDate(testExpiryDate(frontendAppConfig))

    val result = onPageLoadAction(userAnswer, FakeAuthAction())(fakeRequest)

    contentAsString(result) must include(expected)
  }

  behave like controllerWithOnSubmitMethod(onSubmitAction, getEmptyData, None, None)
}
