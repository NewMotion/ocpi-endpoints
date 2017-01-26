package com.thenewmotion.ocpi
package tokens

import common.{OcpiRejectionHandler, ResponseMarshalling}
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.PathMatcher1
import com.thenewmotion.mobilityid.CountryCode
import com.thenewmotion.mobilityid.OperatorIdIso
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.ErrorResp
import msgs.v2_1.CommonTypes.SuccessResp
import msgs.v2_1.CommonTypes.SuccessWithDataResp
import msgs.v2_1.OcpiJsonProtocol._
import msgs.v2_1.OcpiStatusCode.GenericClientFailure
import msgs.v2_1.OcpiStatusCode.GenericSuccess
import msgs.v2_1.Tokens._
import tokens.TokenError._

import scala.concurrent.ExecutionContext

class CpoTokensRoute(
  service: CpoTokensService
) extends JsonApi with ResponseMarshalling {

  private val CountryCodeSegment: PathMatcher1[CountryCode] = Segment.map(CountryCode(_))
  private val OperatorIdSegment: PathMatcher1[OperatorIdIso] = Segment.map(OperatorIdIso(_))
  private def isResourceAccessAuthorized(apiUser: ApiUser, cc: CountryCode, opId: OperatorIdIso) =
    authorize(CountryCode(apiUser.countryCode) == cc && OperatorIdIso(apiUser.partyId) == opId)

  implicit def tokenErrorResp(
    implicit em: ToResponseMarshaller[(StatusCode, ErrorResp)]
  ): ToResponseMarshaller[TokenError] = {
    em.compose[TokenError] { tokenError =>
      val statusCode = tokenError match {
        case _: TokenNotFound => NotFound
        case (_: TokenCreationFailed | _: TokenUpdateFailed) => OK
        case _ => InternalServerError
      }
      statusCode -> ErrorResp(GenericClientFailure, tokenError.reason)
    }
  }

  def route(apiUser: ApiUser)(implicit executionContext: ExecutionContext) =
    handleRejections(OcpiRejectionHandler.Default) {
      pathPrefix(CountryCodeSegment / OperatorIdSegment / Segment) { (cc, opId, tokenUid) =>
        pathEndOrSingleSlash {
          put {
            isResourceAccessAuthorized(apiUser, cc, opId) {
              entity(as[Token]) { token =>
                complete {
                  service.createOrUpdateToken(cc, opId, tokenUid, token).mapRight { created =>
                    (if (created) Created else OK, SuccessResp(GenericSuccess))
                  }
                }
              }
            }
          } ~
          patch {
            isResourceAccessAuthorized(apiUser, cc, opId) {
              entity(as[TokenPatch]) { patch =>
                complete {
                  service.updateToken(cc, opId, tokenUid, patch).mapRight { _ =>
                    SuccessResp(GenericSuccess)
                  }
                }
              }
            }
          } ~
          get {
            isResourceAccessAuthorized(apiUser, cc, opId) {
              complete {
                service.token(cc, opId, tokenUid).mapRight { token =>
                  SuccessWithDataResp(GenericSuccess, None, data = token)
                }
              }
            }
          }
        }
      }
    }
}
