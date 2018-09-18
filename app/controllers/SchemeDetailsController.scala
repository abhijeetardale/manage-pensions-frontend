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

import javax.inject.Inject

import config.FrontendAppConfig
import connectors._
import controllers.actions._
import models.{PsaDetails, Scheme}
import org.joda.time.format.DateTimeFormat
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsSuccess
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.schemeDetails

import scala.concurrent.Future

class SchemeDetailsController @Inject()(appConfig: FrontendAppConfig,
                                        override val messagesApi: MessagesApi,
                                        dataCacheConnector: DataCacheConnector,
                                        schemeDetailsConnector: SchemeDetailsConnector,
                                        authenticate: AuthAction,
                                        getData: DataRetrievalAction) extends FrontendController with I18nSupport {

  private def fullName(psa: PsaDetails) =
    s"${psa.firstName.getOrElse("")} ${psa.middleName.getOrElse("")} ${psa.lastName.getOrElse("")}"

  private val formatter = DateTimeFormat.forPattern("dd MMMM YYYY")

  def onPageLoad(index: Int): Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      dataCacheConnector.fetch(request.externalId).flatMap {
        case None => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
        case Some(jsValue) => (jsValue \ "schemes").validate[Seq[Scheme]] match {
          case JsSuccess(schemes, _) =>
            val pstr = schemes(index - 1).pstr
            val openedDate = schemes(index - 1).openDate.toString(formatter)
            schemeDetailsConnector.getSchemeDetails("pstr", pstr)
              .map(scheme =>
                Ok(schemeDetails(appConfig, scheme.psaSchemeDetails.schemeDetails.schemeName, Some(openedDate),
                    Some(scheme.psaSchemeDetails.psaDetails.map(_.map(fullName)).getOrElse(Seq.empty)))))
          case _ => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
        }
      }
  }
}
