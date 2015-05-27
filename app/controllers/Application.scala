package controllers

// I've put everything in one file, but usually you'd
// split things up more.
//
// Extra dependencies needed for this Play 2.3.9 app:
// - "com.typesafe.slick" %% "slick" % "3.0.0",
// - "com.google.code.findbugs" % "jsr305" % "2.0.3" // Needed by guava

import com.google.common.io.BaseEncoding
import db.SlickDB
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc._

import play.api.Play.current

object UploadController extends Controller {

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

  // https://www.playframework.com/documentation/2.3.x/ScalaJsonInception

  implicit val uploadReads: Reads[Upload] = Json.reads[Upload] // macro

  // https://www.playframework.com/documentation/2.3.x/ScalaJsonHttp

  /**
   * Action to handle JSON upload. Route is:
   * 
   * PUT     /jsonUpload                 controllers.UploadController.jsonUpload
   *
   * Test with:
   *
   * curl -H 'Content-Type: application/json' -X PUT -d '{"name":"abc","data":"YWJjLWRhdGE="}' http://localhost:9000/jsonUpload
   *
   * The maximum JSON size is set to 100 * 1024 = 100kB. You may want to change
   * this to suit your needs.
   */
  def jsonUpload = Action(BodyParsers.parse.json(maxLength = 100 * 1024)) { request =>
    request.body.validate[Upload].fold(
      { errors =>
        // Could do more thorough validation here, if desired
        BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toFlatJson(errors)))
      },
      { upload =>
        import slick.driver.JdbcDriver.api._

        SlickDB.run {
          Tables.uploads += (0, upload.name, upload.data)
        }
        Ok(Json.obj("status" ->"OK", "message" -> "uploaded"))
      }
    )
  }

}

// The Slick part below here is not tested yet.

// Use H2Driver to connect to an H2 database
// slick.jdbc.JdbcBackend
import slick.driver.JdbcDriver.api._

object Tables {

  /**
   * Definition of the UPLOAD table.
   */
  class Upload(tag: Tag) extends Table[(Int, String, Array[Byte])](tag, "UPLOAD") {
    def id = column[Int]("id", O.PrimaryKey) // This is the primary key column
    def name = column[String]("name")
    def data = column[Array[Byte]]("data")
    def * = (id, name, data)
  }
  val uploads = TableQuery[Upload]

}
