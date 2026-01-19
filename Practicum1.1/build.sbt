ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.7"

lazy val root = (project in file("."))
  .settings(
    name := "Practicum1.1",
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core"      % "1.0.0-RC5",
      "org.tpolecat" %% "doobie-hikari"    % "1.0.0-RC5",
      "com.mysql"     % "mysql-connector-j" % "8.3.0",

      "co.fs2"       %% "fs2-io"               % "3.9.4",
      "org.gnieh"    %% "fs2-data-csv"         % "1.11.0",
      "org.gnieh"    %% "fs2-data-csv-generic" % "1.11.0",

      "io.circe" %% "circe-core"    % "0.14.6",
      "io.circe" %% "circe-generic" % "0.14.6",
      "io.circe" %% "circe-parser"  % "0.14.6",

      "org.slf4j" % "slf4j-simple" % "2.0.9"
    )
  )