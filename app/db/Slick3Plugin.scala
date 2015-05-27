package db

import play.api._
import play.api.db._
import slick.dbio._
import slick.jdbc.JdbcBackend._ // May want to use more specific version
import scala.concurrent.Future

class Slick3Plugin(implicit app: Application) extends Plugin {

  var slickDb: DatabaseDef = null

  override def onStart() = {
    // Get DataSource from Play plugin. Can pass db name
    // as argument, otherwise "default".
    val dataSource = DB.getDataSource()
    // Wrap with Slick config
    slickDb = Database.forDataSource(dataSource)
  }

  override def onStop() = {
    slickDb.close()
  }

  override def enabled = true
}

object SlickDB {
  def db(implicit app: Application): DatabaseDef = {
    app.plugin[Slick3Plugin] match {
      case Some(plugin) => plugin.slickDb
      case None => sys.error("Cannot get Slick3Plugin")
    }
  }
  // helper to delegate to DatabaseDef.run method
  def run[R](a: DBIOAction[R, NoStream, Nothing])(implicit app: Application): Future[R] = {
    db(app).run(a)
  }

}
