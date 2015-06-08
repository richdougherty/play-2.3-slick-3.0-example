package controllers

import com.google.common.io.BaseEncoding
import db._
import java.sql.Blob
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc._
import play.api.mvc.MultipartFormData.FilePart
import scala.concurrent.Future

// Import some implicit values
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object UploadController extends Controller {

  /** Standard method to store an upload */
  private def insertUpload(name: String, data: Blob): Future[Int] = {
    import SlickDB.driver.api._
    // Run a query in the database.
    SlickDB.run {
      // Insert a new row and return the automatic id
      // http://slick.typesafe.com/doc/3.0.0/queries.html#inserting
      (Tables.uploads returning Tables.uploads.map(_.id)) += (0, name, data)
    }
  }

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
   * Test by running `curl`:
   *
   * curl -v -X PUT -H 'Content-Type: application/json' -d '{"name":"abc","data":"YWJjLWRhdGE="}' http://localhost:9000/jsonUpload
   *
   * Result will be:
   *
   * {"status":"OK","id":2}
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
        val dataBlob: Blob = new javax.sql.rowset.serial.SerialBlob(upload.data)
        insertUpload(upload.name, dataBlob).map { id =>
          // Map the id of the new upload into the JSON result
          Ok(Json.obj("status" ->"OK", "id" -> id))
        }
      }
    )
  }

  /**
   * Action to handle multipart form upload.
   *
   * Test by creating a file called `xyz.txt` then running `curl`:
   *
   * curl -v -X POST -F file=@xyz.txt http://localhost:9000/multipartUpload
   *
   * Result will be:
   *
   * {"status":"OK","id":2}
   */
  def multipartUpload = Action.async(BodyParsers.parse.multipartFormData) { request =>
    val optFilePart: Option[FilePart[TemporaryFile]] = request.body.file("file")
    optFilePart.fold {
      Future.successful(BadRequest(Json.obj("status" ->"KO", "message" -> "no file")))
    } { filePart: FilePart[TemporaryFile] =>
      val dataBlob: Blob = new TemporaryFileBlob(filePart.ref)
      insertUpload(filePart.filename, dataBlob).map { id =>
        // Map the id of the new upload into the JSON result
        Ok(Json.obj("status" ->"OK", "id" -> id))
      }
    }
  }

}
