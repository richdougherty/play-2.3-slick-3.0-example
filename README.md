This application demonstrates how to use Slick 3.0 in a Play 2.3 application. The usual way to use Slick in a Play application is to use the [Play Slick plugin](https://github.com/playframework/play-slick). The Play Slick plugin supports Slick 2.0 and 2.1 with Play 2.3, but it doesn't support Play 3. This application shows how you can use Slick 3.0 in a Play 2.3 application without using the Play Slick plugin.

### Explanation

* A dependency on Slick 3.0 has been added to [build.sbt](build.sbt).

* The [`SlickDB` object](app/db/Slick3Plugin.scala) provides a simple interface to Slick. It uses a simple plugin to create and clean up some Slick objects whenever the application starts and stops. You may want to override the `driver` value in this class with whatever type of driver you're using in your application.

* The [`Tables` object](app/db/Tables.scala) defines the `UPLOAD` table using the Slick DSL.

* The database is configured with the standard Play `db.default.*` settings in the [`conf/application.conf`](conf/application.conf) file.

* The database is created using an evolutions script in [`conf/evolutions/default/1.sql`](conf/evolutions/default/1.sql). In [`application.conf`](conf/application.conf) the `applyEvolutions.default` setting is has been set so that the database tables are automatically created. You might not want this in production!

* The [`UploadController`](app/controllers/UploadController.scala) demonstrates how to convert a PUT request containing a JSON value into a case class and then insert that case class into the `UPLOAD` table.
