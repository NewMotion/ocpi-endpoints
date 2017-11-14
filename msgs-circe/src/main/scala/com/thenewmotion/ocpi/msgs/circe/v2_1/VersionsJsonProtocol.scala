package com.thenewmotion.ocpi.msgs
package circe.v2_1

import Versions._
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import CommonJsonProtocol._

trait VersionsJsonProtocol {

  implicit val versionNumberE: Encoder[VersionNumber] = stringEncoder(_.toString)
  implicit val versionNumberD: Decoder[VersionNumber] = tryStringDecoder(VersionNumber.apply)

  implicit val versionE: Encoder[Version] = deriveEncoder
  implicit val versionD: Decoder[Version] = deriveDecoder

  implicit val endpointE: Encoder[Endpoint] = deriveEncoder
  implicit val endpointD: Decoder[Endpoint] = deriveDecoder

  implicit val versionDetailsE: Encoder[VersionDetails] = deriveEncoder
  implicit val versionDetailsD: Decoder[VersionDetails] = deriveDecoder
}

object VersionsJsonProtocol extends VersionsJsonProtocol
