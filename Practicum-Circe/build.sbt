ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.7"

val circeVersion = "0.14.10"

lazy val root = (project in file("."))
  .settings(
    name := "Practicum-Circe",
    fork := true,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.5.4",
      "co.fs2"        %% "fs2-core"    % "3.11.0",
      "co.fs2"        %% "fs2-io"      % "3.11.0",
      "org.gnieh"     %% "fs2-data-csv"         % "1.11.1",
      "org.gnieh"     %% "fs2-data-csv-generic" % "1.11.1",

      // JSON (Circe) - Versiones corregidas
      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion
    )
  )