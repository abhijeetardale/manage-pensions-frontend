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

import controllers.ControllerSpecBase
import forms.triage.DoesPSAStartWithATwoFormProvider
import models.triage.DoesPSAStartWithATwo.StartWithA2AndA0
import models.triage.{DoesPSAStartWithATwo, WhatDoYouWantToDo}
import models.triage.WhatDoYouWantToDo.ManageExistingScheme
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Call
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.annotations.Triage
import utils.{FakeNavigator, Navigator}
import views.html.triage.doesPSAStartWithATwo

class DoesPSAStartWithATwoControllerSpec extends ControllerSpecBase with ScalaFutures with MockitoSugar {

  private val onwardRoute = Call("GET", "/dummy")
  private val application = applicationBuilder(Seq[GuiceableModule](bind[Navigator].
    qualifiedWith(classOf[Triage]).toInstance(new FakeNavigator(onwardRoute)))).build()

  private val view = injector.instanceOf[doesPSAStartWithATwo]
  private val formProvider = new DoesPSAStartWithATwoFormProvider()
  private def viewAsString(form: Form[DoesPSAStartWithATwo] = formProvider()): String = view(form)(fakeRequest, messages).toString

  "DoesPSAStartWithATwoController" must {

    "return OK with the view when calling on page load" in {
      val request = addCSRFToken(FakeRequest(GET, routes.DoesPSAStartWithATwoController.onPageLoad().url))
      val result = route(application, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe view(formProvider())(request, messages).toString
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val postRequest = FakeRequest(POST, routes.DoesPSAStartWithATwoController.onSubmit().url).withFormUrlEncodedBody("value" -> "invalid value")
      val boundForm = formProvider().bind(Map("value" -> "invalid value"))
      val result = route(application, postRequest).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

    "redirect to the next page for a valid request" in {
      val postRequest = FakeRequest(POST, routes.DoesPSAStartWithATwoController.onSubmit().url).withFormUrlEncodedBody("value" -> StartWithA2AndA0.toString)
      val result = route(application, postRequest).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe onwardRoute.url
    }
  }

}




