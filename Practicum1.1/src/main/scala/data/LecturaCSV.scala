package data

import cats.effect.IO
import fs2.io.file.{Files, Path}
import fs2.Stream
import fs2.text
import fs2.data.csv._
import fs2.data.csv.generic.semiauto._
import fs2.data.csv.lenient.attemptDecodeUsingHeaders
import models.Movie
import utilities.CSVDecoder._

object LecturaCSV {

  /**
   * Lee un archivo CSV y retorna un Stream de Movies
   * Maneja errores de forma segura
   */
  def readMoviesFromCsv(filePath: Path, separator: Char = ';'): IO[List[Either[CsvException, Movie]]] = {
    implicit val movieDecoder: CsvRowDecoder[Movie, String] = deriveCsvRowDecoder

    Files[IO]
      .readAll(filePath)
      .through(text.utf8.decode)
      .through(attemptDecodeUsingHeaders[Movie](separator = separator))
      .compile
      .toList
  }

  /**
   * Lee CSV como CsvRow genérico para procesamiento flexible
   */
  def readCsvRows(filePath: Path, separator: Char = ';'): Stream[IO, CsvRow[String]] = {
    Files[IO]
      .readAll(filePath)
      .through(text.utf8.decode)
      .through(decodeUsingHeaders[CsvRow[String]](separator = separator))
  }

  /**
   * Lee CSV con manejo explícito de errores
   */
  def readCsvRowsSafe(filePath: Path, separator: Char = ';'): Stream[IO, Either[Throwable, CsvRow[String]]] = {
    Files[IO]
      .readAll(filePath)
      .through(text.utf8.decode)
      .through(decodeUsingHeaders[CsvRow[String]](separator = separator))
      .attempt
  }

  /**
   * Cuenta filas válidas por un campo específico (ej: ID)
   */
  def countValidRows(filePath: Path,
                     fieldName: String,
                     validator: String => Boolean,
                     separator: Char = ';'): IO[Long] = {
    Files[IO]
      .readAll(filePath)
      .through(text.utf8.decode)
      .through(decodeUsingHeaders[CsvRow[String]](separator = separator))
      .map(row => row(fieldName))
      .collect {
        case Some(value) if validator(value) => value
      }
      .compile
      .count
  }

  /**
   * Lee todas las filas y las convierte a Map[String, String]
   */
  def readCsvAsMap(filePath: Path, separator: Char = ';'): IO[List[Map[String, String]]] = {
    Files[IO]
      .readAll(filePath)
      .through(text.utf8.decode)
      .through(decodeUsingHeaders[CsvRow[String]](separator = separator))
      .map(row  => row.toMap)
      .compile
      .toList
  }
}