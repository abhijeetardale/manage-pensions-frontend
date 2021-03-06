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

import connectors._
import connectors.scheme.{ListOfSchemesConnector, PensionSchemeVarianceLockConnector, SchemeDetailsConnector}
import controllers.actions._
import handlers.ErrorHandler
import identifiers.SchemeNameId
import identifiers.invitations.PSTRId
import models._
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers.{contentAsString, _}
import play.twirl.api.Html
import services.SchemeDetailsService
import testhelpers.CommonBuilders._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UserAnswers
import viewmodels.AssociatedPsa
import views.html.{error_template, error_template_page_not_found, schemeDetails}

import scala.concurrent.Future

class SchemeDetailsControllerSpec extends ControllerSpecBase with BeforeAndAfterEach {

  import SchemeDetailsControllerSpec._

  val schemeDetailsView: schemeDetails = app.injector.instanceOf[schemeDetails]
  val errorHandlerView: error_template = app.injector.instanceOf[error_template]
  val errorHandlerNotFoundView: error_template_page_not_found = app.injector.instanceOf[error_template_page_not_found]
  val errorHandler = new ErrorHandler(frontendAppConfig, messagesApi, errorHandlerView, errorHandlerNotFoundView)

  def controller(): SchemeDetailsController = {
    new SchemeDetailsController(
      frontendAppConfig,
      messagesApi,
      fakeSchemeDetailsConnector,
      fakeListOfSchemesConnector,
      fakeSchemeLockConnector,
      FakeAuthAction(),
      FakeUserAnswersCacheConnector,
      errorHandler,
      stubMessagesControllerComponents(),
      schemeDetailsService,
      schemeDetailsView
    )
  }


  override def beforeEach(): Unit = {
    reset(fakeSchemeDetailsConnector, fakeListOfSchemesConnector, fakeSchemeLockConnector, schemeDetailsService)
    when(fakeSchemeLockConnector.isLockByPsaIdOrSchemeId(eqTo("A0000000"), any())(any(), any()))
      .thenReturn(Future.successful(Some(VarianceLock)))
  }

  "SchemeDetailsController" must {

    "return OK and the correct view for a GET" in {
      when(fakeSchemeDetailsConnector.getSchemeDetails(eqTo("A0000000"), any(), any())(any(), any()))
        .thenReturn(Future.successful(desUserAnswers))
      when(fakeListOfSchemesConnector.getListOfSchemes(any())(any(), any()))
        .thenReturn(Future.successful(listOfSchemesResponse))
      when(schemeDetailsService.displayChangeLink(any(), any())).thenReturn(false)
      when(schemeDetailsService.pstr(any(), any())).thenReturn(pstr)
      when(schemeDetailsService.openedDate(any(), any(), any())).thenReturn(openDate)
      when(schemeDetailsService.administratorsVariations(any(), any(), any())).thenReturn(administrators)
      when(schemeDetailsService.lockingPsa(any(), any())(any(), any())).thenReturn(Future.successful(Some("test-psa")))
      when(schemeDetailsService.retrieveAftHtml(any(), any())(any())).thenReturn(Future(Html("test-aft-html")))

      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe schemeDetailsView(schemeName, pstr, openDate, administrators, srn, isSchemeOpen = true,
        displayChangeLink = false, lockingPsa = Some("test-psa"), aftHtml = aftHtml)(fakeRequest, messages).toString()
    }

    "return NOT_FOUND when PSA data is not returned by API (as we don't know who administers the scheme)" in {
      when(fakeSchemeDetailsConnector.getSchemeDetails(eqTo("A0000000"), any(), any())(any(), any()))
        .thenReturn(Future.successful(UserAnswers(Json.obj("psaDetails" -> JsArray()))))

      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe NOT_FOUND
    }

    "return NOT_FOUND and the correct not found view when the selected scheme is not administered by the logged-in PSA" in {
      when(fakeSchemeDetailsConnector.getSchemeDetails(eqTo("A0000000"), any(), any())(any(), any()))
        .thenReturn(Future.successful(UserAnswers(Json.obj("schemeStatus" -> "Open",
          SchemeNameId.toString -> schemeName,
          "psaDetails" -> JsArray(Seq(
            Json.obj(
              "id" -> "A0000007",
              "organisationOrPartnershipName" -> "partnetship name 2",
              "relationshipDate" -> "2018-07-01"
            )))))))

      val result = controller().onPageLoad(srn)(fakeRequest)
      status(result) mustBe NOT_FOUND
      contentAsString(result).contains(messages("messages__pageNotFound404__heading")) mustBe true
    }
  }
}

private object SchemeDetailsControllerSpec extends MockitoSugar {

  private val fakeSchemeDetailsConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]
  private val fakeListOfSchemesConnector: ListOfSchemesConnector = mock[ListOfSchemesConnector]
  private val fakeSchemeLockConnector: PensionSchemeVarianceLockConnector = mock[PensionSchemeVarianceLockConnector]
  private val schemeDetailsService: SchemeDetailsService = mock[SchemeDetailsService]

  private val schemeName = "Benefits Scheme"
  private val pstr = Some("10000678RE")
  private val openDate = Some("10 October 2012")
  private val srn = SchemeReferenceNumber("S1000000456")
  private val aftHtml = Html("test-aft-html")

  private val administrators =
    Some(
      Seq(
        AssociatedPsa("Taylor Middle Rayon", canRemove = true),
        AssociatedPsa("Smith A Tony", canRemove = false)
      )
    )

  private val desUserAnswers = UserAnswers(Json.obj(
    PSTRId.toString -> pstr.get,
    "schemeStatus" -> "Open",
    SchemeNameId.toString -> schemeName,
    "psaDetails" -> JsArray(
      Seq(
        Json.obj(
          "id" -> "A0000000",
          "organisationOrPartnershipName" -> "partnership name 2",
          "relationshipDate" -> "2018-07-01"
        ),
        Json.obj(
          "id" -> "A0000001",
          "individual" -> Json.obj(
            "firstName" -> "Tony",
            "middleName" -> "A",
            "lastName" -> "Smith"
          ),
          "relationshipDate" -> "2018-07-01"
        )
      )
    )
  ))
}
