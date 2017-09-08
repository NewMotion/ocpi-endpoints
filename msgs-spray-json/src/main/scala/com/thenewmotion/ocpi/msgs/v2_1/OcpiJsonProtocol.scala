package com.thenewmotion.ocpi.msgs
package v2_1

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField._
import java.time.{Duration, LocalDate, LocalTime, ZonedDateTime}

import CommonTypes.{BusinessDetails, _}
import Credentials.Creds
import Locations._
import Tokens._
import Versions._
import Tariffs._
import Cdrs._
import com.thenewmotion.ocpi.OcpiDateTimeParser
import spray.json._

trait OcpiJsonProtocol extends DefaultJsonProtocol {

  import reflect._

  private val PASS1 = """([A-Z]+)([A-Z][a-z])""".r
  private val PASS2 = """([a-z\d])([A-Z])""".r
  private val REPLACEMENT = "$1_$2"

  // Convert camelCase to snake_case
  override protected def extractFieldNames(classTag: ClassTag[_]) = {
    import java.util.Locale

    def snakify(name: String) = PASS2.replaceAllIn(PASS1.replaceAllIn(name, REPLACEMENT), REPLACEMENT).toLowerCase(Locale.US)

    super.extractFieldNames(classTag) map snakify
  }

  implicit val ZonedDateTimeOptionalMillisFormat = new JsonFormat[ZonedDateTime] {
    def write (x: ZonedDateTime) = JsString(OcpiDateTimeParser.format(x))
    def read (value: JsValue) = value match {
      case JsString (x) => OcpiDateTimeParser.parseOpt(x) match {
        case Some(parsed) => parsed
        case None => deserializationError ("Expected DateTime conforming to pattern " +
          "specified in OCPI 2.1 section 14.2, but got " + x)
      }
      case x => deserializationError ("Expected DateTime as JsString, but got " + x)
    }
  }

  implicit object localTimeJsonFormat extends JsonFormat[LocalTime] {
    private val fmt: DateTimeFormatter =
      new DateTimeFormatterBuilder()
        .appendValue(HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(MINUTE_OF_HOUR, 2)
        .toFormatter

    def write(c: LocalTime) = JsString(fmt.format(c))
    def read(value: JsValue) = value match {
      case JsString(str) => LocalTime.parse(str, fmt)
      case x => deserializationError("Expected Time as String, but got " + x)
    }
  }

  implicit object localDateJsonFormat extends JsonFormat[LocalDate] {
    private val fmt: DateTimeFormatter =
      new DateTimeFormatterBuilder()
        .appendValue(YEAR, 4)
        .appendLiteral('-')
        .appendValue(MONTH_OF_YEAR, 2)
        .appendLiteral('-')
        .appendValue(DAY_OF_MONTH, 2)
        .toFormatter

    def write(c: LocalDate) = JsString(fmt.format(c))
    def read(value: JsValue) = value match {
      case JsString(str) => LocalDate.parse(str, fmt)
      case x => deserializationError("Expected Date as String, but got " + x)
    }
  }

  implicit val durationFormat = new JsonFormat[Duration] {
    override def write(obj: Duration) = JsNumber(obj.getSeconds)

    override def read(json: JsValue) = json match {
      case JsNumber(n) => java.time.Duration.ofSeconds(n.toLong)
      case x => deserializationError(s"Expected Duration as JsNumber, but got $x")
    }
  }

  implicit val capabilityFormat =
    new SimpleStringEnumSerializer[Capability](Capability).enumFormat

  implicit val connectorStatusFormat =
    new SimpleStringEnumSerializer[ConnectorStatus](ConnectorStatus).enumFormat

  implicit val connectorTypeFormat =
    new SimpleStringEnumSerializer[ConnectorType](ConnectorType).enumFormat

  implicit val connectorFormatFormat =
    new SimpleStringEnumSerializer[ConnectorFormat](ConnectorFormat).enumFormat

  implicit val currentTypeFormat =
    new SimpleStringEnumSerializer[PowerType](PowerType).enumFormat

  implicit val pricingUnitFormat =
    new SimpleStringEnumSerializer[PricingUnit](PricingUnit).enumFormat

  implicit val periodTypeFormat =
    new SimpleStringEnumSerializer[PeriodType](PeriodType).enumFormat

  implicit val locationTypeFormat =
    new SimpleStringEnumSerializer[LocationType](LocationType).enumFormat

  implicit val parkingRestrictionTypeFormat =
    new SimpleStringEnumSerializer[ParkingRestriction](ParkingRestriction).enumFormat

  implicit val imageCategoryTypeFormat =
    new SimpleStringEnumSerializer[ImageCategory](ImageCategory).enumFormat

  implicit val facilityTypeFormat =
    new SimpleStringEnumSerializer[Facility](Facility).enumFormat

  implicit val energySourceCategoryTypeFormat =
    new SimpleStringEnumSerializer[EnergySourceCategory](EnergySourceCategory).enumFormat

  implicit val environmentalImpactCategoryTypeFormat =
    new SimpleStringEnumSerializer[EnvironmentalImpactCategory](EnvironmentalImpactCategory).enumFormat

  implicit val tokenTypeFormat =
    new SimpleStringEnumSerializer[TokenType](TokenType).enumFormat

  implicit val whitelistTypeFormat =
    new SimpleStringEnumSerializer[WhitelistType](WhitelistType).enumFormat

  implicit val energySourceFormat = jsonFormat2(EnergySource)
  implicit val environmentalImpactFormat = jsonFormat2(EnvironmentalImpact)

  implicit val energyMixFormat = new JsonFormat[EnergyMix] {
    val readFormat = jsonFormat5(EnergyMix.deserialize)
    val writeFormat = jsonFormat5(EnergyMix.apply)
    override def read(json: JsValue) = readFormat.read(json)
    override def write(obj: EnergyMix): JsValue = writeFormat.write(obj)
  }
  implicit val powerFormat = jsonFormat3(Power)
  implicit val displayTestFormat = jsonFormat2(DisplayText)
  implicit val geoLocationFormat = jsonFormat2(GeoLocation)
  implicit val additionalGeoLocationFormat = jsonFormat3(AdditionalGeoLocation)
  implicit val regularHoursFormat = jsonFormat3(RegularHours.apply(_: Int, _: LocalTime, _: LocalTime))
  implicit val exceptionalPeriodFormat = jsonFormat2(ExceptionalPeriod)
  implicit val hoursFormat = new JsonFormat[Hours] {
    val readFormat = jsonFormat4(Hours.deserialize)
    val writeFormat = jsonFormat4(Hours.apply)
    override def read(json: JsValue) = readFormat.read(json)
    override def write(obj: Hours): JsValue = writeFormat.write(obj)
  }
  implicit val urlFmt = new JsonFormat[Url] {
    override def read(json: JsValue) = json match {
      case JsString(s) => Url(s)
      case _ => deserializationError("Url must be a string")
    }
    override def write(obj: Url) = JsString(obj.value)
  }
  implicit val imageFormat = jsonFormat6(Image)
  implicit val businessDetailsFormat = jsonFormat3(BusinessDetails)

  implicit val connectorFormat = jsonFormat9(Connector)
  implicit val connectorPatchFormat = jsonFormat8(ConnectorPatch)
  implicit val statusScheduleFormat = jsonFormat3(StatusSchedule)
  implicit val evseFormat = new RootJsonFormat[Evse] {
    val readFormat = jsonFormat13(Evse.deserialize)
    val writeFormat = jsonFormat13(Evse.apply)
    override def read(json: JsValue) = readFormat.read(json)
    override def write(obj: Evse): JsValue = writeFormat.write(obj)
  }
  implicit val evsePatchFormat = jsonFormat12(EvsePatch)
  implicit val operatorFormat = jsonFormat3(Operator)
  implicit val countryCodeFmt = new JsonFormat[CountryCode] {
    override def read(json: JsValue) = json match {
      case JsString(s) => CountryCode(s)
      case _ => deserializationError("Country Code must be a string")
    }
    override def write(obj: CountryCode) = JsString(obj.value)
  }
  implicit val locationFormat = new RootJsonFormat[Location] {
    val readFormat = jsonFormat21(Location.deserialize)
    val writeFormat = jsonFormat21(Location.apply)
    override def read(json: JsValue) = readFormat.read(json)
    override def write(obj: Location): JsValue = writeFormat.write(obj)
  }
  implicit val languageFmt = new JsonFormat[Language] {
    override def read(json: JsValue) = json match {
      case JsString(s) => Language(s)
      case _ => deserializationError("Language must be a string")
    }
    override def write(obj: Language) = JsString(obj.value)
  }
  implicit val tokensFormat = jsonFormat9(Token)

  implicit val locationPatchFormat = jsonFormat21(LocationPatch)
  implicit val tokenPatchFormat = jsonFormat9(TokenPatch)

  implicit def statusCodeFormat[T <: OcpiStatusCode : ClassTag] = new JsonFormat[T] {
    override def read(json: JsValue): T = json match {
      case JsNumber(x) => OcpiStatusCode(x.toInt) match {
        case y: T => y
        case _ => deserializationError(s"StatusCode $x is not of type ${classTag[T].runtimeClass.getSimpleName}")
      }
      case x => deserializationError("Expected StatusCode as JsNumber, but got " + x)
    }
    override def write(obj: T): JsValue = JsNumber(obj.code)
  }

  implicit val endpointIdentifierFormat = new JsonFormat[EndpointIdentifier] {
    override def write(obj: EndpointIdentifier) = JsString(obj.value)

    override def read(json: JsValue) = json match {
      case JsString(s) => EndpointIdentifier(s)
      case x => deserializationError(s"Expected EndpointIdentifier as JsString, but got $x")
    }
  }

  implicit val versionNumberFormat = new JsonFormat[VersionNumber] {
    override def write(obj: VersionNumber) = JsString(obj.toString)

    override def read(json: JsValue) = json match {
      case JsString(s) => VersionNumber(s)
      case x => deserializationError(s"Expected VersionNumber as JsString, but got $x")
    }
  }

  implicit def tokenFormat[O <: Ownership] = new JsonFormat[AuthToken[O]] {
    override def read(json: JsValue) = json match {
      case JsString(x) => AuthToken[O](x)
      case x => deserializationError(s"Expected AuthToken as JsString, but got $x")
    }
    override def write(obj: AuthToken[O]) = JsString(obj.value)
  }

  implicit val versionFormat = jsonFormat2(Version)
  implicit val endpointFormat = jsonFormat2(Endpoint)
  implicit val versionDetailsFormat = jsonFormat2(VersionDetails)
  implicit val errorRespFormat = jsonFormat3(ErrorResp)
  implicit val successRespFormat = jsonFormat3(SuccessResp)
  implicit def successRespWithDataFormat[D : JsonFormat] = jsonFormat4(SuccessWithDataResp[D])

  implicit def credentialsFormat[O <: Ownership] = new RootJsonFormat[Creds[O]] {
    private object Fields {
      val token = "token"
      val url = "url"
      val businessDetails = "business_details"
      val partyId = "party_id"
      val countryCode = "country_code"
    }

    override def write(obj: Creds[O]) =
      JsObject(
        Fields.token -> tokenFormat.write(obj.token),
        Fields.url -> urlFmt.write(obj.url),
        Fields.businessDetails -> businessDetailsFormat.write(obj.businessDetails),
        Fields.partyId -> JsString(obj.globalPartyId.partyId),
        Fields.countryCode -> JsString(obj.globalPartyId.countryCode)
      )

    override def read(json: JsValue) = {
      val token = fromField[AuthToken[O#Opposite]](json, Fields.token)
      val url = fromField[Url](json, Fields.url)
      val businessDetails = fromField[BusinessDetails](json, Fields.businessDetails)
      val partyId = fromField[String](json, Fields.partyId)
      val countryCode = fromField[String](json, Fields.countryCode)

      Creds[O](token, url, businessDetails, GlobalPartyId(countryCode, partyId))
    }
  }

  implicit val locationReferencesFormat = new RootJsonFormat[LocationReferences] {
    val readFormat = jsonFormat3(LocationReferences.deserialize)
    val writeFormat = jsonFormat3(LocationReferences.apply)
    override def read(json: JsValue) = readFormat.read(json)
    override def write(obj: LocationReferences): JsValue = writeFormat.write(obj)
  }

  implicit val allowedFormat =
    new SimpleStringEnumSerializer[Allowed](Allowed).enumFormat

  implicit val authorizationInfoFormat = jsonFormat3(AuthorizationInfo)

  implicit val authMethodFormat =
    new SimpleStringEnumSerializer[AuthMethod](AuthMethod).enumFormat

  implicit val currencyCodeFormat = new JsonFormat[CurrencyCode] {
    override def write(obj: CurrencyCode) = JsString(obj.value)

    override def read(json: JsValue) = json match {
      case JsString(s) => CurrencyCode(s)
      case x => deserializationError(s"Expected CurrencyCode as JsString, but got $x")
    }
  }

  implicit val tariffDimensionTypeFormat =
    new SimpleStringEnumSerializer[TariffDimensionType](TariffDimensionType).enumFormat

  implicit val dayOfWeekFormat =
    new SimpleStringEnumSerializer[DayOfWeek](DayOfWeek).enumFormat

  implicit val priceComponentFormat = jsonFormat3(PriceComponent)

  implicit val tariffRestrictionsFormat = jsonFormat11(TariffRestrictions)

  implicit val tariffElementFormat = jsonFormat2(TariffElement)

  implicit val tariffFormat = jsonFormat7(Tariff)

  implicit val cdrDimensionTypeFormat =
    new SimpleStringEnumSerializer[CdrDimensionType](CdrDimensionType).enumFormat

  implicit val cdrDimensionFormat = jsonFormat2(CdrDimension)

  implicit val chargingPeriodFormat = jsonFormat2(ChargingPeriod)

  implicit val cdrFormat = jsonFormat16(Cdr)
}

object OcpiJsonProtocol extends OcpiJsonProtocol
