package com.thenewmotion.ocpi
package sessions

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import cats.Applicative
import com.thenewmotion.ocpi.common._
import com.thenewmotion.ocpi.msgs.OcpiStatusCode._
import com.thenewmotion.ocpi.msgs.v2_1.Sessions.{Session, SessionId, SessionPatch}
import com.thenewmotion.ocpi.msgs.{ErrorResp, GlobalPartyId, SuccessResp}
import com.thenewmotion.ocpi.sessions.SessionError.{IncorrectSessionId, SessionNotFound}


object SessionsRoute {
  def apply[F[_]: Applicative: HktMarshallable](
    service: SessionsService[F]
  )(
    implicit locationU: FromEntityUnmarshaller[Session],
    locationPU: FromEntityUnmarshaller[SessionPatch],
    errorM: ErrRespMar,
    successUnitM: SuccessRespMar[Unit],
    successLocM: SuccessRespMar[Session]
  ): SessionsRoute[F] = new SessionsRoute(service)
}

class SessionsRoute[F[_]: Applicative: HktMarshallable] private[ocpi] (
  service: SessionsService[F]
)(
  implicit locationU: FromEntityUnmarshaller[Session],
  locationPU: FromEntityUnmarshaller[SessionPatch],
  errorM: ErrRespMar,
  successUnitM: SuccessRespMar[Unit],
  successLocM: SuccessRespMar[Session]
) extends EitherUnmarshalling
    with OcpiDirectives {

  implicit def sessionErrorResp(
    implicit em: ToResponseMarshaller[(StatusCode, ErrorResp)]
  ): ToResponseMarshaller[SessionError] = {
    em.compose[SessionError] { sessionError =>
      val statusCode = sessionError match {
        case (_: SessionNotFound)    => NotFound
        case (_: IncorrectSessionId) => BadRequest
      }
      statusCode -> ErrorResp(GenericClientFailure, sessionError.reason)
    }
  }

  def apply(
    apiUser: GlobalPartyId
  ): Route =
    handleRejections(OcpiRejectionHandler.Default)(routeWithoutRh(apiUser))

  private val SessionIdSegment = Segment.map(SessionId(_))

  import HktMarshallableSyntax._

  private[sessions] def routeWithoutRh(
    apiUser: GlobalPartyId
  ) = {
    (authPathPrefixGlobalPartyIdEquality(apiUser) & pathPrefix(SessionIdSegment)) { sessionId =>
      pathEndOrSingleSlash {
        put {
          entity(as[Session]) { session =>
            complete {
              service.createOrUpdateSession(apiUser, sessionId, session).mapRight { x =>
                (x.httpStatusCode, SuccessResp(GenericSuccess))
              }
            }
          }
        } ~
        patch {
          entity(as[SessionPatch]) { session =>
            complete {
              service.updateSession(apiUser, sessionId, session).mapRight { _ =>
                SuccessResp(GenericSuccess)
              }
            }
          }
        } ~
        get {
          complete {
            service.session(apiUser, sessionId).mapRight { location =>
              SuccessResp(GenericSuccess, None, data = location)
            }
          }
        }
      }
    }
  }
}
