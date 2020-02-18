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

package services

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset}

import base.SpecBase
import connectors.admin.MinimalPsaConnector
import connectors.scheme.{PensionSchemeVarianceLockConnector, UpdateSchemeCacheConnector}
import connectors.{UserAnswersCacheConnector, _}
import controllers.routes.ListSchemesController
import models._
import models.requests.OptionalDataRequest
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsNumber, JsObject, Json}
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testhelpers.InvitationBuilder.{invitation1, invitationList}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HeaderCarrier
import utils.UserAnswers
import viewmodels.{CardViewModel, Message}

import scala.concurrent.Future
import scala.language.postfixOps

class SchemesOverviewServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with ScalaFutures {

  import SchemesOverviewServiceSpec._

  private val dataCacheConnector: UserAnswersCacheConnector = mock[MicroserviceCacheConnector]
  private val minimalPsaConnector: MinimalPsaConnector = mock[MinimalPsaConnector]
  private val lockConnector = mock[PensionSchemeVarianceLockConnector]
  private val updateConnector = mock[UpdateSchemeCacheConnector]
  private val invitationsCacheConnector = mock[InvitationsCacheConnector]

  override def beforeEach(): Unit = {
    when(minimalPsaConnector.getPsaNameFromPsaID(eqTo(psaId))(any(), any()))
      .thenReturn(Future.successful(minimalPsaName))
    when(invitationsCacheConnector.getForInvitee(any())(any(), any()))
      .thenReturn(Future.successful(invitationList))
    when(dataCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(Some(schemeNameJsonOption)))
    when(dataCacheConnector.lastUpdated(any())(any(), any()))
      .thenReturn(Future.successful(Some(JsNumber(BigDecimal(timestamp)))))

    when(lockConnector.getLockByPsa(any())(any(), any()))
      .thenReturn(Future.successful(Some(SchemeVariance(psaId, srn))))
    when(updateConnector.fetch(any())(any(), any()))
      .thenReturn(Future.successful(Some(schemeNameJsonOption)))
    when(updateConnector.lastUpdated(any())(any(), any()))
      .thenReturn(Future.successful(Some(JsNumber(BigDecimal(timestamp)))))
    super.beforeEach()
  }

  def service: SchemesOverviewService =
    new SchemesOverviewService(frontendAppConfig, dataCacheConnector, minimalPsaConnector,
      lockConnector, updateConnector, invitationsCacheConnector)

  "getTiles" must {

    "return tiles with relevant links" when {
      "when all possible links are displayed" in {

        whenReady(service.getTiles(psaId)) {
          _ mustBe tiles()
        }

      }

      "psa is not invited to any schemes" in {
        when(invitationsCacheConnector.getForInvitee(any())(any(), any()))
          .thenReturn(Future.successful(Nil))

        whenReady(service.getTiles(psaId)) {
          _ mustBe tiles(adminCard(invitation = noInvitationsLink))
        }
      }

      "psa is invited to administer only one scheme" in {
        when(invitationsCacheConnector.getForInvitee(any())(any(), any()))
          .thenReturn(Future.successful(List(invitation1)))

        whenReady(service.getTiles(psaId)) {
          _ mustBe tiles(adminCard(invitation = oneInvitationsLink))
        }
      }

      "when there is no ongoing subscription" in {
        when(dataCacheConnector.fetch(any())(any(), any())).thenReturn(Future.successful(None))

        whenReady(service.getTiles(psaId)) {
          _ mustBe tiles(scheme = schemeCard(registerLink))
        }
      }

      "when there is no lock for any scheme" in {
        when(lockConnector.getLockByPsa(eqTo(psaId))(any(), any())).thenReturn(Future.successful(None))

        whenReady(service.getTiles(psaId)) {
          _ mustBe tiles(scheme = schemeCard(schemeVariationLinks = Nil))
        }
      }

      "when there is a lock for a scheme but the scheme is not in the update collection" in {
         when(updateConnector.fetch(any())(any(), any()))
          .thenReturn(Future.successful(None))

        whenReady(service.getTiles(psaId)) {
          _ mustBe tiles(scheme = schemeCard(schemeVariationLinks = Nil))
        }
      }

    }
  }

    "checkIfSchemeCanBeRegistered" must {

      "redirect to the cannot start registration page if called without a psa name but psa is suspended" in {
        when(minimalPsaConnector.getMinimalPsaDetails(eqTo(psaId))(any(), any())).thenReturn(Future.successful(minimalPsaDetails(true)))
        when(dataCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(None))

        val result = service.checkIfSchemeCanBeRegistered(psaId)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe cannotStartRegistrationUrl.url
      }

      "redirect to the register scheme page if called without psa name but psa is not suspended" in {
        when(minimalPsaConnector.getMinimalPsaDetails(eqTo(psaId))(any(), any())).thenReturn(Future.successful(minimalPsaDetails(false)))
        when(dataCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(None))

        val result = service.checkIfSchemeCanBeRegistered(psaId)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe frontendAppConfig.registerSchemeUrl
      }

      "redirect to continue register a scheme page if called with a psa name and psa is not suspended" in {
        when(minimalPsaConnector.getMinimalPsaDetails(eqTo(psaId))(any(), any())).thenReturn(Future.successful(minimalPsaDetails(false)))
        when(dataCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(Some(schemeNameJsonOption)))

        val result = service.checkIfSchemeCanBeRegistered(psaId)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe frontendAppConfig.continueSchemeUrl
      }

      "redirect to cannot start registration page if called with a psa name and psa is suspended" in {
        when(minimalPsaConnector.getMinimalPsaDetails(eqTo(psaId))(any(), any())).thenReturn(Future.successful(minimalPsaDetails(true)))
        when(dataCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(Some(schemeNameJsonOption)))

        val result = service.checkIfSchemeCanBeRegistered(psaId)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe cannotStartRegistrationUrl.url
      }


      "redirect to cannot start registration page if  scheme details are found with scheme name missing and srn number present" in {
        when(dataCacheConnector.fetch(eqTo("id"))(any(), any())).thenReturn(Future.successful(schemeSrnNumberOnlyData))
        when(dataCacheConnector.removeAll(eqTo("id"))(any(), any())).thenReturn(Future(Ok))
        when(minimalPsaConnector.getMinimalPsaDetails(eqTo(psaId))(any(), any())).thenReturn(Future.successful(minimalPsaDetails(false)))


        val result = service.checkIfSchemeCanBeRegistered(psaId)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe frontendAppConfig.registerSchemeUrl
        verify(dataCacheConnector, times(1)).removeAll(any())(any(), any())
      }
    }

}

object SchemesOverviewServiceSpec extends SpecBase with MockitoSugar  {

  implicit val request: OptionalDataRequest[AnyContent] =
    OptionalDataRequest(FakeRequest("", ""), "id", Some(UserAnswers()), PsaId("A0000000"))
  implicit val hc: HeaderCarrier = HeaderCarrier()




  val psaName: String = "John Doe"
  val schemeName = "Test Scheme Name"
  val timestamp: Long = System.currentTimeMillis
  private val psaId = "A0000000"
  private val srn = "srn"
  private val formatter = DateTimeFormatter.ofPattern("dd MMMM YYYY")

  val deleteDate = LocalDate.now(ZoneOffset.UTC).plusDays(frontendAppConfig.daysDataSaved).format(formatter)

  def minimalPsaDetails(psaSuspended: Boolean) = MinimalPSA("test@test.com", psaSuspended, Some("Org Name"), None)

  val minimalPsaName = Some("John Doe Doe")
  val minimalPsaOrgName = Some("Org Name")
  val expectedPsaOrgName = Some("Org Name")
  val individualPsaDetailsWithNoMiddleName = Some("John Doe")
  val minimalPsaDetailsOrg = MinimalPSA("test@test.com", isPsaSuspended = false, Some("Org Name"), None)
  val expectedName: String = "John Doe Doe"

  val cannotStartRegistrationUrl: Call = controllers.routes.CannotStartRegistrationController.onPageLoad()

  val schemeNameJsonOption: JsObject = Json.obj("schemeName" -> schemeName)
  val schemeSrnNumberOnlyData = Some(Json.obj("submissionReferenceNumber" -> Json.obj("schemeReferenceNumber" -> srn)))

  private def adminCard(deregistration: Seq[Link] = deregisterLink,
                        invitation: Seq[Link] = invitationsLink) = CardViewModel(
    id = "administrator-card",
    heading = Message("messages__schemeOverview__psa_heading"),
    subHeading = Some(Message("messages__schemeOverview__psa_id")),
    subHeadingParam = Some(psaId),
    links = Seq(
      Link("psaLink", frontendAppConfig.registeredPsaDetailsUrl, Message("messages__schemeOverview__psa_change"))
    ) ++ invitation ++ deregistration)

  private def schemeCard(schemeSubscriptionLinks: Seq[Link] = subscriptionLinks,
                         schemeVariationLinks: Seq[Link] = variationLinks) = CardViewModel(
    id = "scheme-card",
    heading = Message("messages__schemeOverview__scheme_heading"),
    links = Seq(
      Link("view-schemes", ListSchemesController.onPageLoad().url, Message("messages__schemeOverview__scheme_view"))
    ) ++ schemeSubscriptionLinks ++ schemeVariationLinks
  )

  private val deregisterLink = Seq(Link("deregister-link", controllers.deregister.routes.ConfirmStopBeingPsaController.onPageLoad().url,
    Message("messages__schemeOverview__psa_deregister")))

  private val invitationsLink = Seq(Link("invitations-received", controllers.invitations.routes.YourInvitationsController.onPageLoad().url,
    Message("messages__schemeOverview__psa_view_more_invitations", 2)))

  private val noInvitationsLink = Seq(Link("invitations-received", controllers.invitations.routes.YourInvitationsController.onPageLoad().url,
    Message("messages__schemeOverview__psa_view_no_invitation")))

  private val oneInvitationsLink = Seq(Link("invitations-received", controllers.invitations.routes.YourInvitationsController.onPageLoad().url,
    Message("messages__schemeOverview__psa_view_one_invitation")))

  private val registerLink = Seq(Link("register-new-scheme", controllers.routes.SchemesOverviewController.onClickCheckIfSchemeCanBeRegistered().url,
    Message("messages__schemeOverview__scheme_subscription")))

  private val subscriptionLinks = Seq(Link("continue-registration", controllers.routes.SchemesOverviewController.onClickCheckIfSchemeCanBeRegistered().url,
    Message("messages__schemeOverview__scheme_subscription_continue", schemeName, deleteDate)),
  Link("delete-registration", controllers.routes.DeleteSchemeController.onPageLoad().url,
    Message("messages__schemeOverview__scheme_subscription_delete", schemeName)))

  private val variationLinks = Seq(Link("continue-variation", frontendAppConfig.viewSchemeDetailsUrl.format(srn),
    Message("messages__schemeOverview__scheme_variations_continue", schemeName, deleteDate)),
    Link("delete-variation", controllers.routes.DeleteSchemeChangesController.onPageLoad(srn).url,
      Message("messages__schemeOverview__scheme_variations_delete", schemeName)))

  private def tiles(admin: CardViewModel = adminCard(), scheme: CardViewModel = schemeCard()): Seq[CardViewModel] = Seq(admin, scheme)
}



