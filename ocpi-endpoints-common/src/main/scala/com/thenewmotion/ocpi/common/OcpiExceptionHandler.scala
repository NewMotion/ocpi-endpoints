package com.thenewmotion.ocpi.common

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server.directives.BasicDirectives
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.ErrorResp
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCode._

object OcpiExceptionHandler extends BasicDirectives with SprayJsonSupport {

  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  val Default = ExceptionHandler {

    case exception => complete {
      ( OK,
        ErrorResp(
          GenericServerFailure,
          Some(exception.toString)))
    }
  }
}
