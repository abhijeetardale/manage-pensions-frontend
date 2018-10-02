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

package utils.navigators

import connectors.UserAnswersCacheConnector
import controllers.routes
import identifiers.SchemeSrnId
import identifiers.invitations._
import javax.inject.{Inject, Singleton}
import models.NormalMode
import utils.{Navigator, UserAnswers}

@Singleton
class AcceptInvitationNavigator @Inject()(val dataCacheConnector: UserAnswersCacheConnector) extends Navigator {

  override def routeMap(from: NavigateFrom): Option[NavigateTo] = from.id match {
    case SchemeSrnId =>
      NavigateTo.dontSave(controllers.invitations.routes.HaveYouEmployedPensionAdviserController.onPageLoad(NormalMode))
    case HaveYouEmployedPensionAdviserId =>
      normalAdviserRoutes(from.userAnswers)
    case AdviserNameId =>
      NavigateTo.dontSave(controllers.invitations.routes.AdviserEmailAddressController.onPageLoad(NormalMode))
    case AdviserEmailId =>
      NavigateTo.dontSave(controllers.invitations.routes.AdviserAddressPostcodeLookupController.onPageLoad())
    case AdviserAddressPostCodeLookupId =>
      NavigateTo.dontSave(controllers.invitations.routes.PensionAdviserAddressListController.onPageLoad(NormalMode))
    case AdviserAddressListId =>
      NavigateTo.dontSave(controllers.invitations.routes.AdviserManualAddressController.onPageLoad(NormalMode, true))
    case AdviserAddressId =>
      NavigateTo.dontSave(controllers.invitations.routes.CheckPensionAdviserAnswersController.onPageLoad())
    case DeclarationId => NavigateTo.dontSave(routes.IndexController.onPageLoad())

    case _ => NavigateTo.dontSave(controllers.routes.SessionExpiredController.onPageLoad())
  }

  private def normalAdviserRoutes(userAnswers: UserAnswers) = {
    userAnswers.get(HaveYouEmployedPensionAdviserId) match {
      case Some(true) =>
        NavigateTo.dontSave(controllers.invitations.routes.AdviserDetailsController.onPageLoad(NormalMode))
      case Some(false) =>
        NavigateTo.dontSave(controllers.invitations.routes.DeclarationController.onPageLoad())
      case _ =>
        NavigateTo.dontSave(controllers.routes.SessionExpiredController.onPageLoad())
    }
  }

  override protected def editRouteMap(from: NavigateFrom): Option[NavigateTo] = from.id match {
    case _ => NavigateTo.dontSave(controllers.routes.SessionExpiredController.onPageLoad())
  }
}
