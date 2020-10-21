package com.thenewmotion.ocpi
package common

import _root_.akka.http.scaladsl.model.StatusCodes._
import _root_.akka.http.scaladsl.server.Directives._
import _root_.akka.http.scaladsl.server.ExceptionHandler
import _root_.akka.http.scaladsl.server.directives.BasicDirectives
import msgs.ErrorResp
import msgs.OcpiStatusCode._

object OcpiExceptionHandler extends BasicDirectives {

  protected val logger = Logger(getClass)

  def Default(
    implicit m: ErrRespMar
  ) = ExceptionHandler {
    case exception => extractRequest { request =>
      logger.error(s"An error occurred processing: ${HttpLogging.redactHttpRequest(request)}", exception)
      complete {
        ( OK,
          ErrorResp(
            GenericServerFailure,
            Some(exception.toString)))
      }
    }
  }
}
