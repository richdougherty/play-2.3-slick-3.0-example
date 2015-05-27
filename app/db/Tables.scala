package db

// http://slick.typesafe.com/doc/3.0.0/schemas.html

/**
 * Table definitions.
 */
object Tables {

  import SlickDB.driver.api._

  /**
   * Definition of the UPLOAD table.
   */
  class Upload(tag: Tag) extends Table[(Int, String, Array[Byte])](tag, "UPLOAD") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def data = column[Array[Byte]]("DATA")
    def * = (id, name, data)
  }
  val uploads = TableQuery[Upload]

}
