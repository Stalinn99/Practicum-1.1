error id: file:///C:/Users/Lenin/Desktop/Practicum-1.1/Practicum1.1/src/main/scala/data/LecturaJSON.scala:DateTimeFormatter.
file:///C:/Users/Lenin/Desktop/Practicum-1.1/Practicum1.1/src/main/scala/data/LecturaJSON.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -models/DateTimeFormatter.
	 -models/DateTimeFormatter#
	 -models/DateTimeFormatter().
	 -java/time/format/DateTimeFormatter.
	 -java/time/format/DateTimeFormatter#
	 -java/time/format/DateTimeFormatter().
	 -DateTimeFormatter.
	 -DateTimeFormatter#
	 -DateTimeFormatter().
	 -scala/Predef.DateTimeFormatter.
	 -scala/Predef.DateTimeFormatter#
	 -scala/Predef.DateTimeFormatter().
offset: 306
uri: file:///C:/Users/Lenin/Desktop/Practicum-1.1/Practicum1.1/src/main/scala/data/LecturaJSON.scala
text:
```scala
package data

import cats.effect.IO
import fs2.io.file.{Files, Path}
import io.circe.Decoder
import utilities.Parsear_JSON
import models.*
import fs2.data.csv.lenient.attemptDecodeUsingHeaders
import io.circe.generic.auto.deriveDecoder

import java.time.LocalDate
import java.time.format.DateTim@@eFormatter
import scala.util.Try

object LecturaJSON {


  case class RatingsStats(
                           totalRatings: Long,
                           totalUserIds: Long,
                           usuariosUnicos: Long,
                           peliculasConRatings: Long,
                           promedioRatingsPorPelicula: Double
                         )

  case class FechaStats(
                         totalFilas: Long,
                         fechasValidas: Long,
                         fechasInvalidas: Long,
                         ejemplosFechasParseadas: List[(String, String)]
                       )

  case class AnalisisGeneral(
                              titulo: String,
                              totalUnicos: Int,
                              totalRelaciones: Long,
                              topItems: List[(String, Int)]
                            )


  /**
   * Función base para analizar campos JSON que contienen LISTAS
   * Ejemplo: genres, crew, cast, keywords, etc.
   */
  def analyzeJsonField[T](
                           filePath: Path,
                           fieldName: String,
                           extractKey: T => String,
                           separator: Char = ';'
                         )(implicit decoder: Decoder[List[T]]): IO[Map[String, Int]] = {

    Files[IO].readAll(filePath)
      .through(fs2.text.utf8.decode)
      .through(attemptDecodeUsingHeaders[Map[String, String]](separator = separator))
      .collect { case Right(row) => row }
      .map { row =>
        val jsonRaw = row.getOrElse(fieldName, "[]")
        val parsed = Parsear_JSON.parseJsonField[T](jsonRaw)
        parsed.groupMapReduce(extractKey)(_ => 1)(_ + _)
      }
      .fold(Map.empty[String, Int]) { (acc, rowMap) =>
        rowMap.foldLeft(acc) { case (currentAcc, (k, v)) =>
          currentAcc.updated(k, currentAcc.getOrElse(k, 0) + v)
        }
      }
      .compile
      .lastOrError
  }

  /**
   * Función base para analizar campos JSON que contienen OBJETOS ÚNICOS
   * Ejemplo: belongs_to_collection (un solo objeto por película)
   */
  def analyzeJsonFieldSingle[T](
                                 filePath: Path,
                                 fieldName: String,
                                 extractKey: T => String,
                                 separator: Char = ';'
                               )(implicit decoder: Decoder[T]): IO[Map[String, Int]] = {

    Files[IO].readAll(filePath)
      .through(fs2.text.utf8.decode)
      .through(attemptDecodeUsingHeaders[Map[String, String]](separator = separator))
      .collect { case Right(row) => row }
      .map { row =>
        val jsonRaw = row.getOrElse(fieldName, "{}")
        Parsear_JSON.parseJsonFieldSingle[T](jsonRaw) match {
          case Some(item) => Map(extractKey(item) -> 1)
          case None => Map.empty[String, Int]
        }
      }
      .fold(Map.empty[String, Int]) { (acc, rowMap) =>
        if (rowMap.isEmpty) acc
        else {
          val (k, v) = rowMap.head
          acc.updated(k, acc.getOrElse(k, 0) + v)
        }
      }
      .compile
      .lastOrError
  }

  // ============= FUNCIONES DE ANÁLISIS ESPECÍFICAS =============

  def analyzeGenres(filePath: Path, separator: Char = ';'): IO[Map[String, Int]] =
    analyzeJsonField[Genres](filePath, "genres", _.name, separator)

  def analyzeCrewByJob(filePath: Path, separator: Char = ';'): IO[Map[String, Int]] =
    analyzeJsonField[Crew](filePath, "crew", _.job, separator)

  def analyzeCrewByDepartment(filePath: Path, separator: Char = ';'): IO[Map[String, Int]] =
    analyzeJsonField[Crew](filePath, "crew", _.department, separator)

  def analyzeCastByName(filePath: Path, separator: Char = ';'): IO[Map[String, Int]] =
    analyzeJsonField[Cast](filePath, "cast", _.name, separator)

  def analisisKeyWords(filePath: Path, separator: Char = ';'): IO[Map[String, Int]] =
    analyzeJsonField[Keywords](filePath, "keywords", _.name, separator)

  def analisisSpokenLenguaje(filePath: Path, separator: Char = ';'): IO[Map[String, Int]] =
    analyzeJsonField[Spoken_Languages](filePath, "spoken_languages", _.name, separator)

  def analisisColecciones(filePath: Path, separator: Char = ';'): IO[Map[String, Int]] =
    analyzeJsonFieldSingle[BelongToCollection](filePath, "belongs_to_collection", _.name, separator)

  def analisisCompanias(filePath: Path, separator: Char = ';'): IO[Map[String, Int]] =
    analyzeJsonField[Production_Companies](filePath, "production_companies", _.name, separator)

  def analisisPaises(filePath: Path, separator: Char = ';'): IO[Map[String, Int]] =
    analyzeJsonField[Production_Countries](filePath, "production_countries", _.name, separator)

  // ============= FUNCIONES DE CONTEO Y RATINGS =============

  def contarTotalUserIds(filePath: Path, separator: Char = ';'): IO[Long] = {
    Files[IO].readAll(filePath)
      .through(fs2.text.utf8.decode)
      .through(attemptDecodeUsingHeaders[Map[String, String]](separator = separator))
      .collect { case Right(row) => row }
      .map { row =>
        val ratingsJson = row.getOrElse("ratings", "[]")
        Parsear_JSON.parseJsonField[Ratings](ratingsJson).length.toLong
      }
      .compile
      .fold(0L)(_ + _)
  }

  def contarUsuariosUnicos(filePath: Path, separator: Char = ';'): IO[Long] = {
    Files[IO].readAll(filePath)
      .through(fs2.text.utf8.decode)
      .through(attemptDecodeUsingHeaders[Map[String, String]](separator = separator))
      .collect { case Right(row) => row }
      .map { row =>
        val ratingsJson = row.getOrElse("ratings", "[]")
        Parsear_JSON.parseJsonField[Ratings](ratingsJson).map(_.userId)
      }
      .compile
      .toList
      .map(_.flatten.distinct.length.toLong)
  }

  def contarTotalRatings(filePath: Path, separator: Char = ';'): IO[Long] =
    contarTotalUserIds(filePath, separator)

  // ============= FUNCIONES DE FECHAS =============

  def parsearFechaEstreno(fechaStr: String): Option[String] = {
    val targetFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
    val formatters = List(
      DateTimeFormatter.ofPattern("yyyy-MM-dd"),
      DateTimeFormatter.ofPattern("yyyy/MM/dd"),
      DateTimeFormatter.ofPattern("dd/MM/yyyy"),
      DateTimeFormatter.ISO_LOCAL_DATE,
      DateTimeFormatter.ofPattern("yyyy")
    )

    if (fechaStr == null || fechaStr.trim.isEmpty || fechaStr.equalsIgnoreCase("null")) return None

    val cleanStr = fechaStr.trim

    formatters.foldLeft[Option[LocalDate]](None) { (result, formatter) =>
      result.orElse {
        Try(LocalDate.parse(cleanStr, formatter)).toOption
          .orElse(if (cleanStr.matches("\\d{4}")) Try(LocalDate.of(cleanStr.toInt, 1, 1)).toOption else None)
      }
    }.map(_.format(targetFormatter))
  }

  def analizarFechasEstreno(filePath: Path, separator: Char = ';'): IO[FechaStats] = {
    Files[IO].readAll(filePath)
      .through(fs2.text.utf8.decode)
      .through(attemptDecodeUsingHeaders[Map[String, String]](separator = separator))
      .collect { case Right(row) => row }
      .map { row =>
        val fechaOriginal = row.getOrElse("release_date", "")
        val fechaParseada = parsearFechaEstreno(fechaOriginal)
        (fechaOriginal, fechaParseada)
      }
      .compile
      .toList
      .map { fechas =>
        val total = fechas.length.toLong
        val validas = fechas.count(_._2.isDefined).toLong
        FechaStats(total, validas, total - validas, fechas.collect { case (o, Some(p)) => (o, p) }.take(10))
      }
  }

  def analisisRatings(filePath: Path, separator: Char = ';'): IO[RatingsStats] = {
    for {
      total <- contarTotalRatings(filePath, separator)
      unicos <- contarUsuariosUnicos(filePath, separator)
      peliculasConRatings <- Files[IO].readAll(filePath)
        .through(fs2.text.utf8.decode)
        .through(attemptDecodeUsingHeaders[Map[String, String]](separator = separator))
        .collect { case Right(row) => row }
        .map { row =>
          val json = row.getOrElse("ratings", "[]")
          if (json.length > 5 && json != "[]") 1L else 0L
        }
        .compile.fold(0L)(_ + _)

    } yield RatingsStats(
      totalRatings = total,
      totalUserIds = total,
      usuariosUnicos = unicos,
      peliculasConRatings = peliculasConRatings,
      promedioRatingsPorPelicula = if (peliculasConRatings > 0) total.toDouble / peliculasConRatings else 0.0
    )
  }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 