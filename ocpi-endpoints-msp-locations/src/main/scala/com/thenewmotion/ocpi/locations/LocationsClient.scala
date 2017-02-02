package com.thenewmotion.ocpi
package locations

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import com.thenewmotion.ocpi.common.{OcpiClient, PaginatedSource}
import com.thenewmotion.ocpi.msgs.v2_1.Locations.Location
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import scala.concurrent.{ExecutionContext, Future}
import scalaz._
import com.github.nscala_time.time.Imports._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.ErrorResp

class LocationsClient(implicit actorSystem: ActorSystem) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  def getLocations(uri: Uri, auth: String, dateFrom: Option[DateTime] = None, dateTo: Option[DateTime] = None)
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[ErrorResp \/ Iterable[Location]] =
    traversePaginatedResource[Location](uri, auth, dateFrom, dateTo)

  def locationsSource(uri: Uri, auth: String, dateFrom: Option[DateTime] = None, dateTo: Option[DateTime] = None)
                  (implicit ec: ExecutionContext, mat: ActorMaterializer): Source[Location, NotUsed] =
    PaginatedSource[Location](http, uri, auth, dateFrom, dateTo)

}