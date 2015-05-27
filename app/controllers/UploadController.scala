package controllers

import com.google.common.io.BaseEncoding
import db._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc._
import scala.concurrent.Future

// Import some implicit values
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object UploadController extends Controller {

  import SlickDB.driver.api._

  // https://www.playframework.com/documentation/2.3.x/ScalaJsonCombinators

  /**
   * Tell the Play JSON library how to turn JSON into a byte array.
   */
  implicit object Base64Reads extends Reads[Array[Byte]] {
    def reads(json: JsValue) = json match {
      case JsString(s) =>
        try {
          val bytes = BaseEncoding.base64().decode(s)
          JsSuccess(bytes)
        } catch {
          case _: IllegalArgumentException =>
            JsError("error.expected.base64")
        }
      case _ => JsError("error.expected.jsstring")
    }
  }

  /** Use a case class to hold the data that is uploaded. */
  case class Upload(
    name: String,
    data: Array[Byte]
  )

  // Use a macro to generate code for converting JSON into an Upload
  // https://www.playframework.com/documentation/2.3.x/ScalaJsonInception

  implicit val uploadReads: Reads[Upload] = Json.reads[Upload] // macro

  // https://www.playframework.com/documentation/2.3.x/ScalaJsonHttp

  /**
   * Action to handle JSON upload.
   *
   * Test with:
   *
   * curl -H 'Content-Type: application/json' -X PUT -d '{"name":"abc","data":"YWJjLWRhdGE="}' http://localhost:9000/jsonUpload
   *
   * Result will be:
   *
   * {"status":"OK","id":2
   *
   * The maximum JSON size is set to 100 * 1024 = 100kB. You may want to change
   * this to suit your needs.
   */
  def jsonUpload = Action.async(BodyParsers.parse.json(maxLength = 100 * 1024)) { request =>
    request.body.validate[Upload].fold(
      { errors =>
        // Could do more thorough validation here, if desired
        Future.successful(BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toFlatJson(errors))))
      },
      { upload =>
        // Run a query in the database.
        SlickDB.run {
          // Insert a new row and return the automatic id
          // http://slick.typesafe.com/doc/3.0.0/queries.html#inserting
          (Tables.uploads returning Tables.uploads.map(_.id)) += (0, upload.name, upload.data)
        }.map { id =>
          // Map the id of the new upload into the JSON result
          Ok(Json.obj("status" ->"OK", "id" -> id))
        }
      }
    )
  }

}
