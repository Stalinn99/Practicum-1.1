ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.7"

lazy val root = (project in file("."))
  .settings(
    name := "Practicum1.1",
    libraryDependencies ++= Seq(
      // --- EFECTOS Y STREAMS ---
      "org.typelevel" %% "cats-effect" % "3.5.3",
      "co.fs2"        %% "fs2-core"    % "3.9.3",
      "co.fs2"        %% "fs2-io"      % "3.9.3",

      // --- CSV (Añadido fs2-data-csv-generic para derivación automática) ---
      "org.gnieh"     %% "fs2-data-csv"         % "1.9.1",
      "org.gnieh"     %% "fs2-data-csv-generic" % "1.9.1",

      // --- BASE DE DATOS (Añadido Driver de MySQL) ---
      "org.tpolecat"  %% "doobie-core"      % "1.0.0-RC4",
      "org.tpolecat"  %% "doobie-hikari"    % "1.0.0-RC4",
      "mysql"          % "mysql-connector-java" % "8.0.33",

      // --- JSON (CIRCE) ---
      "io.circe"      %% "circe-core"    % "0.14.6",
      "io.circe"      %% "circe-generic" % "0.14.6",
      "io.circe"      %% "circe-parser"  % "0.14.6"
    )
  )