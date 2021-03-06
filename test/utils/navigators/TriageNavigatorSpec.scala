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

package utils.navigators

import base.SpecBase
import identifiers.Identifier
import identifiers.triage._
import models.triage.DoesPSAStartWithATwo.{No, StartWithA2AndA0, Yes}
import models.triage.{DoesPSAStartWithATwo, WhatDoYouWantToDo}
import models.triage.WhatDoYouWantToDo.{BecomeAnAdmin, ChangeAdminDetails, CheckTheSchemeStatus, Invite, ManageExistingScheme, UpdateSchemeInformation}
import org.scalatest.OptionValues
import org.scalatest.prop.TableFor4
import play.api.libs.json.Json
import play.api.mvc.Call
import utils.{Enumerable, NavigatorBehaviour, UserAnswers}

class TriageNavigatorSpec extends SpecBase with NavigatorBehaviour {

  import TriageNavigatorSpec._

  private val navigator = new TriageNavigator(frontendAppConfig)

  private def loginToYourSchemesPage: Call = Call("GET", s"${frontendAppConfig.loginUrl}?continue=${frontendAppConfig.loginToListSchemesUrl}")

  private def loginToChangePsaDetailsPage: Call = Call("GET", s"${frontendAppConfig.loginUrl}?continue=${frontendAppConfig.registeredPsaDetailsUrl}")

  private def tpssWelcomePage: Call = Call("GET", frontendAppConfig.tpssWelcomeUrl)

  private def tpssInitialQuestionsPage: Call = Call("GET", frontendAppConfig.tpssInitialQuestionsUrl)

  private def pensionSchemesInvitationGuideGovUkPage: Call = Call("GET", frontendAppConfig.pensionSchemesInvitationGuideGovUkLink)

  private def pensionSchemesGuideGovUkPage: Call = Call("GET", frontendAppConfig.pensionSchemesGuideGovUkLink)

  def routes(): TableFor4[Identifier, UserAnswers, Call, Option[Call]] = Table(
    ("Id", "User Answers", "Next Page (NormalMode)", "Next Page (CheckMode)"),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswers(ManageExistingScheme), doesPSTRTStartWithTwoPage, None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswers(CheckTheSchemeStatus), loginToYourSchemesPage, None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswers(Invite), doesPSTRTStartWithTwoInvitePage, None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswers(BecomeAnAdmin), doesPSTRTStartWithTwoInvitedPage, None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswers(UpdateSchemeInformation), doesPSTRTStartWithTwoUpdatePage, None),
    (WhatDoYouWantToDoId, whatDoYouWantToDoAnswers(ChangeAdminDetails), doesPSATStartWithATwoPage, None),
    (WhatDoYouWantToDoId, emptyAnswers, sessionExpiredPage, None),
    (DoesPSTRStartWithTwoId, doesPSTRStartWithTwoAnswers(true), loginToYourSchemesPage, None),
    (DoesPSTRStartWithTwoId, doesPSTRStartWithTwoAnswers(false), tpssWelcomePage, None),
    (DoesPSTRStartWithTwoId, emptyAnswers, sessionExpiredPage, None),
    (DoesPSTRStartWithTwoInviteId, doesPSTRStartWithTwoInviteAnswers(true), invitingPSTRStartWithTwoPage, None),
    (DoesPSTRStartWithTwoInviteId, doesPSTRStartWithTwoInviteAnswers(false), pensionSchemesInvitationGuideGovUkPage, None),
    (DoesPSTRStartWithTwoInviteId, emptyAnswers, sessionExpiredPage, None),
    (DoesPSTRStartWithTwoUpdateId, doesPSTRStartWithTwoUpdateAnswers(true), updatingPSTRStartWithTwoPage, None),
    (DoesPSTRStartWithTwoUpdateId, doesPSTRStartWithTwoUpdateAnswers(false), pensionSchemesGuideGovUkPage, None),
    (DoesPSTRStartWithTwoUpdateId, emptyAnswers, sessionExpiredPage, None),
    (DoesPSAStartWithATwoId, doesPSAStartWithATwoAnswers(Yes), loginToChangePsaDetailsPage, None),
    (DoesPSAStartWithATwoId, doesPSAStartWithATwoAnswers(No), tpssInitialQuestionsPage, None),
    (DoesPSAStartWithATwoId, doesPSAStartWithATwoAnswers(StartWithA2AndA0), aTwoAndAZeroIdsPage, None),
    (DoesPSAStartWithATwoId, emptyAnswers, sessionExpiredPage, None)
  )

  navigator.getClass.getSimpleName must {
    appRunning()
    behave like nonMatchingNavigator(navigator)
    behave like navigatorWithRoutes(navigator, routes(), dataDescriber)
  }
}

object TriageNavigatorSpec extends OptionValues with Enumerable.Implicits {

  lazy val emptyAnswers = UserAnswers(Json.obj())

  private def whatDoYouWantToDoAnswers(answer: WhatDoYouWantToDo): UserAnswers = UserAnswers().set(WhatDoYouWantToDoId)(answer).asOpt.value

  private def doesPSAStartWithATwoAnswers(answer: DoesPSAStartWithATwo): UserAnswers = UserAnswers().set(DoesPSAStartWithATwoId)(answer).asOpt.value

  private def doesPSTRStartWithTwoAnswers(answer: Boolean): UserAnswers = UserAnswers().set(DoesPSTRStartWithTwoId)(answer).asOpt.value

  private def doesPSTRStartWithTwoInviteAnswers(answer: Boolean): UserAnswers = UserAnswers().set(DoesPSTRStartWithTwoInviteId)(answer).asOpt.value

  private def doesPSTRStartWithTwoUpdateAnswers(answer: Boolean): UserAnswers = UserAnswers().set(DoesPSTRStartWithTwoUpdateId)(answer).asOpt.value

  private def doesPSTRTStartWithTwoPage: Call = controllers.triage.routes.DoesPSTRStartWithTwoController.onPageLoad()

  private def doesPSTRTStartWithTwoInvitePage: Call = controllers.triage.routes.DoesPSTRStartWithTwoInviteController.onPageLoad()

  private def doesPSTRTStartWithTwoInvitedPage: Call = controllers.triage.routes.DoesPSTRStartWithTwoInvitedController.onPageLoad()

  private def doesPSTRTStartWithTwoUpdatePage: Call = controllers.triage.routes.DoesPSTRStartWithTwoUpdateController.onPageLoad()

  private def doesPSATStartWithATwoPage: Call = controllers.triage.routes.DoesPSAStartWithATwoController.onPageLoad()

  private def invitingPSTRStartWithTwoPage: Call = controllers.triage.routes.InvitingPSTRStartWithTwoController.onPageLoad()

  private def updatingPSTRStartWithTwoPage: Call = controllers.triage.routes.UpdatingPSTRStartWithTwoController.onPageLoad()

  private def aTwoAndAZeroIdsPage: Call = controllers.triage.routes.ATwoAndAZeroIdsController.onPageLoad()

  private def sessionExpiredPage: Call = controllers.routes.SessionExpiredController.onPageLoad()


  private def dataDescriber(answers: UserAnswers): String = answers.toString
}


