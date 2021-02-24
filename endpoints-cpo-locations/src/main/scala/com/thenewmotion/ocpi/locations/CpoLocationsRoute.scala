package com.thenewmotion.ocpi
package locations

import java.time.ZonedDateTime
import _root_.akka.http.scaladsl.marshalling.ToResponseMarshaller
import _root_.akka.http.scaladsl.model.StatusCode
import _root_.akka.http.scaladsl.model.StatusCodes._
import _root_.akka.http.scaladsl.server.Route
import cats.effect.Effect
import com.thenewmotion.ocpi.common._
import com.thenewmotion.ocpi.locations.LocationsError._
import com.thenewmotion.ocpi.msgs.OcpiStatusCode.{GenericClientFailure, GenericSuccess}
import com.thenewmotion.ocpi.msgs.v2_1.Locations._
import com.thenewmotion.ocpi.msgs.{ErrorResp, GlobalPartyId, SuccessResp}

object CpoLocationsRoute {
  def apply[F[_]: Effect: HktMarshallable](
    service: CpoLocationsService[F],
    DefaultLimit: Int = 1000,
    MaxLimit: Int = 1000,
    currentTime: => ZonedDateTime = ZonedDateTime.now,
    linkHeaderScheme: Option[String] = None
  )(
    implicit errorM: ErrRespMar,
    successIterableLocM: SuccessRespMar[Iterable[Location]],
    successLocM: SuccessRespMar[Location],
    successEvseM: SuccessRespMar[Evse],
    successConnM: SuccessRespMar[Connector]
  ) = new CpoLocationsRoute(service, DefaultLimit, MaxLimit, currentTime, linkHeaderScheme)
}

class CpoLocationsRoute[F[_]: Effect: HktMarshallable] private[ocpi](
  service: CpoLocationsService[F],
  val DefaultLimit: Int,
  val MaxLimit: Int,
  currentTime: => ZonedDateTime,
  override val linkHeaderScheme: Option[String] = None
)(
  implicit errorM: ErrRespMar,
  successIterableLocM: SuccessRespMar[Iterable[Location]],
  successLocM: SuccessRespMar[Location],
  successEvseM: SuccessRespMar[Evse],
  successConnM: SuccessRespMar[Connector]
) extends OcpiDirectives
    with PaginatedRoute
    with EitherUnmarshalling {

  private val DefaultErrorMsg = Some("An error occurred.")

  implicit def locationsErrorResp(
    implicit em: ToResponseMarshaller[(StatusCode, ErrorResp)]
  ): ToResponseMarshaller[LocationsError] = {
    em.compose[LocationsError] { locationsError =>
      val statusCode = locationsError match {
        case _: LocationNotFound | _: EvseNotFound | _: ConnectorNotFound => NotFound
        case _                                                            => InternalServerError
      }
      statusCode -> ErrorResp(GenericClientFailure, locationsError.reason.orElse(DefaultErrorMsg))
    }
  }

  def apply(
    apiUser: GlobalPartyId
  ): Route =
    handleRejections(OcpiRejectionHandler.Default)(routeWithoutRh(apiUser))

  private val LocationIdSegment = Segment.map(LocationId(_))
  private val EvseUidSegment = Segment.map(EvseUid(_))
  private val ConnectorIdSegment = Segment.map(ConnectorId(_))

  import HktMarshallableSyntax._

  private[locations] def routeWithoutRh(
    apiUser: GlobalPartyId
  ) = {
    get {
      pathEndOrSingleSlash {
        paged { (pager: Pager, dateFrom: Option[ZonedDateTime], dateTo: Option[ZonedDateTime]) =>
          onSuccess(Effect[F].toIO(service.locations(pager, dateFrom, dateTo)).unsafeToFuture()) {
            _.fold(complete(_), pagLocations => {
              respondWithPaginationHeaders(pager, pagLocations) {
                complete {
                  SuccessResp(GenericSuccess, data = pagLocations.result)
                }
              }
            })
          }
        }
      } ~
      pathPrefix(LocationIdSegment) { locId =>
        pathEndOrSingleSlash {
          complete {
            service.location(locId).mapRight { location =>
              SuccessResp(GenericSuccess, data = location)
            }
          }
        } ~
        pathPrefix(EvseUidSegment) { evseId =>
          pathEndOrSingleSlash {
            complete {
              service.evse(locId, evseId).mapRight { evse =>
                SuccessResp(GenericSuccess, data = evse)
              }
            }
          } ~
          (path(ConnectorIdSegment) & pathEndOrSingleSlash) { connId =>
            complete {
              service.connector(locId, evseId, connId).mapRight { connector =>
                SuccessResp(GenericSuccess, data = connector)
              }
            }
          }
        }
      }
    }
  }
}
