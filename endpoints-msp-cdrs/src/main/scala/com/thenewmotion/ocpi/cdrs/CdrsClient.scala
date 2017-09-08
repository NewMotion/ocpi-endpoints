package com.thenewmotion.ocpi
package cdrs

import java.time.ZonedDateTime

import akka.NotUsed
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.Uri
import common.{OcpiClient, PaginatedSource}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import scala.concurrent.{ExecutionContext, Future}
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import com.thenewmotion.ocpi.msgs.v2_1.Cdrs.Cdr
import msgs.AuthToken

class CdrsClient(implicit http: HttpExt) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  def getCdrs(uri: Uri, auth: AuthToken[Ours], dateFrom: Option[ZonedDateTime] = None, dateTo: Option[ZonedDateTime] = None)
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[ErrorRespOr[Iterable[Cdr]]] =
    traversePaginatedResource[Cdr](uri, auth, dateFrom, dateTo)

  def cdrsSource(uri: Uri, auth: AuthToken[Ours], dateFrom: Option[ZonedDateTime] = None, dateTo: Option[ZonedDateTime] = None)
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Source[Cdr, NotUsed] =
    PaginatedSource[Cdr](http, uri, auth, dateFrom, dateTo)

}
