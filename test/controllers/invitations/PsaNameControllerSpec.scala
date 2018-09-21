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

package controllers.invitations

import connectors.FakeDataCacheConnector
import controllers.ControllerSpecBase
import controllers.actions._
import forms.PsaNameFormProvider
import identifiers.invitations.PsaNameId
import models.NormalMode
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.Helpers._
import utils.FakeNavigator
import views.html.invitations.psaName

class PsaNameControllerSpec extends ControllerSpecBase {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new PsaNameFormProvider()
  val form = formProvider()

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyData) =
    new PsaNameController(frontendAppConfig, messagesApi, FakeDataCacheConnector, new FakeNavigator(onwardRoute), FakeAuthAction(),
      dataRetrievalAction, new DataRequiredActionImpl, formProvider)

  def viewAsString(form: Form[_] = form) = psaName(frontendAppConfig, form, NormalMode)(fakeRequest, messages).toString

  val testAnswer = ("answer")

  "PsaName Controller" must {

    "return OK and the correct view for a GET" in {
      val result = controller().onPageLoad(NormalMode)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val validData = Json.obj(PsaNameId.toString -> "answer")
      val getRelevantData = new FakeDataRetrievalAction(Some(validData))

      val result = controller(getRelevantData).onPageLoad(NormalMode)(fakeRequest)

      contentAsString(result) mustBe viewAsString(form.fill(testAnswer))
    }

    "redirect to the next page when valid data is submitted" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody(("psaName", "answer"))

      val result = controller().onSubmit(NormalMode)(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val postRequest = fakeRequest.withFormUrlEncodedBody(("psaName", ""))
      val boundForm = form.bind(Map("psaName" -> ""))

      val result = controller().onSubmit(NormalMode)(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }
  }
}