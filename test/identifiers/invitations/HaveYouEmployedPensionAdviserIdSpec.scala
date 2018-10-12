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

package identifiers.invitations

import models.{Address, TolerantAddress}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import utils.UserAnswers

class HaveYouEmployedPensionAdviserIdSpec extends WordSpec with MustMatchers with OptionValues {

  val tolerantAddressSample = Seq(
    TolerantAddress(Some("10 Other Place"), Some("Some District"), Some("Anytown"), Some("Somerset"), Some("ZZ1 1ZZ"), Some("UK"))
  )
  val userAnswers = UserAnswers().havePensionAdviser(true).
    adviserName("test").
    adviserEmail("test@test.com").
    adviserPostCodeLookup(Seq(TolerantAddress(Some("10 Other Place"), Some("Some District"), Some("Anytown"), Some("Somerset"), Some("ZZ1 1ZZ"), Some("UK")))).
    adviserAddressList(TolerantAddress(Some("10 Other Place"), Some("Some District"), Some("Anytown"), Some("Somerset"), Some("ZZ1 1ZZ"), Some("UK"))).
    adviserAddress(Address("10 Other Place", "Some District", None, None, Some("ZZ1 1ZZ"), "UK"))

  "Clean up" when {

    "setting have you employed pension adviser to false" must {
      val result = userAnswers.havePensionAdviser(false)

      "remove Adviser name" in {

        result.get(AdviserNameId) mustNot be(defined)
      }

      "remove Adviser email" in {

        result.get(AdviserEmailId) mustNot be(defined)
      }

      "remove Adviser post code lookup" in {

        result.get(AdviserAddressPostCodeLookupId) mustNot be(defined)
      }

      "remove Adviser address list" in {

        result.get(AdviserAddressListId) mustNot be(defined)
      }

      "remove Adviser address" in {

        result.get(AdviserAddressId) mustNot be(defined)
      }
    }

    "setting have you employed pension adviser to true" must {
      val result = userAnswers.set(HaveYouEmployedPensionAdviserId)(true).asOpt.value

      "not remove Adviser name" in {

        result.get(AdviserNameId) must be(defined)
      }

      "not remove Adviser email" in {

        result.get(AdviserEmailId) must be(defined)
      }

      "not remove Adviser post code lookup" in {

        result.get(AdviserAddressPostCodeLookupId) must be(defined)
      }

      "not remove Adviser address list" in {

        result.get(AdviserAddressListId) must be(defined)
      }

      "not remove Adviser address" in {

        result.get(AdviserAddressId) must be(defined)
      }
    }
  }
}