package db

import play.api._
import play.api.db._
import scala.concurrent.Future
import slick.dbio._
import slick.driver._
import slick.util.AsyncExecutor

/**
 * Interface to the Slick database operations. This class uses the
 * SlickDBPlugin to find the current active database.
 */
object SlickDB {

  // Replace with the right kind of driver, e.g. PostgresDriver
  val driver = H2Driver

  /** Get the Slick Database */
  def db(implicit app: Application): driver.type#Backend#Database = {
    app.plugin[SlickDBPlugin] match {
      case Some(plugin) => plugin.slickDb
      case None => sys.error("Cannot get SlickDBPlugin")
    }
  }

  /** helper to delegate to db.run method */
  def run[R](a: DBIOAction[R, NoStream, Nothing])(implicit app: Application): Future[R] = {
    db(app).run(a)
  }

}

/**
 * Use a plugin to create and clean up the Slick objects.
 *
 * https://www.playframework.com/documentation/2.3.x/ScalaPlugins
 */
class SlickDBPlugin(implicit app: Application) extends Plugin {

  var slickDb: SlickDB.driver.Backend#Database = null

  override def onStart() = {
    // Get DataSource from Play plugin. Can pass db name
    // as argument, otherwise "default".
    val dataSource = DB.getDataSource()
    // Configure a thread pool for the database operations.
    // Below we use the default values, but it should be
    // tuned for your application. Read about the config
    // here: http://slick.typesafe.com/doc/3.0.0/database.html#database-thread-pool
    val executor = AsyncExecutor(
      name = "SlickDBPluginExecutor",
      numThreads = 20,
      queueSize = 1000
    )
    // Get a Slick database wrapper
    slickDb = SlickDB.driver.backend.Database.forDataSource(dataSource, executor)
  }

  override def onStop() = {
    // Free up Slick resources when the application shuts down
    slickDb.close()
  }

  override def enabled = true
}