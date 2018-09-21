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

package forms.behaviours

import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}
import play.api.data.{Form, FormError}

trait FieldBehaviours extends WordSpec with Matchers with PropertyChecks {

  lazy val emptyForm = Map[String, String]()

  def fieldThatBindsValidData(form: Form[_],
                              fieldName: String,
                              validDataGenerator: Gen[String]): Unit = {

    "bind valid data" in {

      forAll(validDataGenerator.retryUntil(!_.matches("""^\s+$""")) -> "validDataItem") {
        dataItem: String =>
          val result = form.bind(Map(fieldName -> dataItem)).apply(fieldName)
          result.errors shouldBe empty
      }
    }
  }

  def mandatoryField(form: Form[_],
                     fieldName: String,
                     requiredError: FormError): Unit = {

    "not bind when key is not present at all" in {

      val result = form.bind(emptyForm).apply(fieldName)
      result.errors shouldEqual Seq(requiredError)
    }

    "not bind blank values" in {

      val result = form.bind(Map(fieldName -> "")).apply(fieldName)
      result.errors shouldEqual Seq(requiredError)
    }
  }

}