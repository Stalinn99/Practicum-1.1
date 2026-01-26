error id: file:///C:/Users/Lenin/Desktop/Practicum-1.1/Practicum1.1/src/main/scala/data/LecturaCSV.scala:io.
file:///C:/Users/Lenin/Desktop/Practicum-1.1/Practicum1.1/src/main/scala/data/LecturaCSV.scala
empty definition using pc, found symbol in pc: io.
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -fs2/data/csv/generic/semiauto/fs2/io.
	 -utilities/CSVDecoder.fs2.io.
	 -fs2/io.
	 -scala/Predef.fs2.io.
offset: 50
uri: file:///C:/Users/Lenin/Desktop/Practicum-1.1/Practicum1.1/src/main/scala/data/LecturaCSV.scala
text:
```scala
package data

import cats.effect.IO
import fs2.@@io.file.{Files, Path}
import fs2.data.csv.lenient.attemptDecodeUsingHeaders
import fs2.data.csv.generic.semiauto.*
import models.Movie
import utilities.CSVDecoder.*

object LecturaCSV {

  /**
   * Lee un archivo CSV y retorna un Stream de Movies
   * Maneja errores de forma segura
   */
  def readMoviesFromCsv(filePath: Path, separator: Char = ';'): IO[List[Either[String, Movie]]] = {
    import fs2.data.csv.CsvRowDecoder

    implicit val movieDecoder: CsvRowDecoder[Movie, String] = deriveCsvRowDecoder[Movie]

    Files[IO]
      .readAll(filePath)
      .through(fs2.text.utf8.decode)
      .through(attemptDecodeUsingHeaders[Movie](separator = separator))
      .map {
        case Right(movie) => Right(movie)
        case Left(error) => Left(error.getMessage)
      }
      .compile
      .toList
  }

  /**
   * Cuenta filas válidas por un campo específico (ej: ID)
   * Usa attemptDecodeUsingHeaders
   */
  def countValidRows(
                      filePath: Path,
                      fieldName: String,
                      validator: String => Boolean,
                      separator: Char = ';'
                    ): IO[Long] = {
    Files[IO]
      .readAll(filePath)
      .through(fs2.text.utf8.decode)
      .through(attemptDecodeUsingHeaders[Map[String, String]](separator = separator))
      .collect { case Right(row) => row }
      .map(row => row.getOrElse(fieldName, ""))
      .filter(validator)
      .compile
      .count
  }

  /**
   * Lee todas las filas y las convierte a Map[String, String]
   * Maneja filas corruptas
   */
  def readCsvAsMap(filePath: Path, separator: Char = ';'): IO[List[Map[String, String]]] = {
    Files[IO]
      .readAll(filePath)
      .through(fs2.text.utf8.decode)
      .through(attemptDecodeUsingHeaders[Map[String, String]](separator = separator))
      .collect { case Right(row) => row }
      .compile
      .toList
  }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: io.