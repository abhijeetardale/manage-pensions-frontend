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

package controllers.triage

import config.FrontendAppConfig
import controllers.actions.TriageAction
import forms.triage.DoesPSTRStartWithTwoFormProvider
import identifiers.triage.DoesPSTRStartWithTwoUpdateId
import javax.inject.Inject
import models.NormalMode
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.FrontendBaseController
import utils.annotations.Triage
import utils.{Navigator, UserAnswers}
import views.html.triage.doesPSTRStartWithTwo

import scala.concurrent.{ExecutionContext, Future}

class DoesPSTRStartWithTwoUpdateController @Inject()(
                                                      appConfig: FrontendAppConfig,
                                                      override val messagesApi: MessagesApi,
                                                      @Triage navigator: Navigator,
                                                      triageAction: TriageAction,
                                                      formProvider: DoesPSTRStartWithTwoFormProvider,
                                                      val controllerComponents: MessagesControllerComponents,
                                                      view: doesPSTRStartWithTwo
                                                    )(implicit val ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form: Form[Boolean] = formProvider()

  private def hint(implicit messages: Messages) = Some(messages("messages__doesPSTRStartWithTwo_update__hint"))
  private def postCall: Call = controllers.triage.routes.DoesPSTRStartWithTwoUpdateController.onSubmit()

  def onPageLoad: Action[AnyContent] = triageAction.async {
    implicit request =>
      Future.successful(Ok(view(form, postCall, hint)))
  }

  def onSubmit: Action[AnyContent] = triageAction.async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(view(formWithErrors, postCall, hint))),
        value => {
          val uaUpdated = UserAnswers().set(DoesPSTRStartWithTwoUpdateId)(value).asOpt.getOrElse(UserAnswers())
          Future.successful(Redirect(navigator.nextPage(DoesPSTRStartWithTwoUpdateId, NormalMode, uaUpdated)(request, implicitly, implicitly)))
        }
      )
  }
}
