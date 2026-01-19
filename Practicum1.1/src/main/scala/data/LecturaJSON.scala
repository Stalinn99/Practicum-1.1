package data

import cats.effect.IO
import fs2.Stream
import fs2.io.file.Path
import fs2.data.csv.CsvRow
import io.circe.Decoder
import io.circe.generic.auto.*
import utilities.Parsear_JSON

import javax.swing.JToolBar.Separator

object LecturaJSON {

  /**
   * Analiza un campo JSON en un CSV y cuenta frecuencias
   * Genérico para cualquier tipo T que tenga un Decoder de Circe
   */
  def analyzeJsonField[T](
                           filePath: Path,
                           fieldName: String,
                           extractKey: T => String,
                           separator: Char = ';'
                         )(implicit decoder: Decoder[List[T]]): IO[Map[String, Int]] = {

    LecturaCSV.readCsvRows(filePath, separator)
      .map { row =>
        val jsonRaw = row(fieldName).getOrElse("[]")
        val parsed = Parsear_JSON.parseJsonField[T](jsonRaw)

        // Contar frecuencias en esta fila
        parsed.foldLeft(Map.empty[String, Int]) { (acc, item) =>
          val key = extractKey(item)
          acc + (key -> (acc.getOrElse(key, 0) + 1))
        }
      }
      // Combinar todos los mapas de conteo
      .fold(Map.empty[String, Int]) { (accMap, rowMap) =>
        rowMap.foldLeft(accMap) { case (acc, (key, count)) =>
          acc + (key -> (acc.getOrElse(key, 0) + count))
        }
      }
      .compile
      .lastOrError
  }

  /**
   * Extrae un mapeo de clave -> valor desde un campo JSON
   */
  def extractJsonMapping[T](
                             filePath: Path,
                             fieldName: String,
                             keyField: T => String,
                             valueField: T => String,
                             separator: Char = ';'
                           )(implicit decoder: Decoder[List[T]]): IO[Map[String, String]] = {

    LecturaCSV.readCsvRows(filePath, separator)
      .map { row =>
        val jsonRaw = row(fieldName).getOrElse("[]")
        val parsed = Parsear_JSON.parseJsonField[T](jsonRaw)

        // Crear un mapa de esta fila: p.ej. "en" -> "English"
        parsed.map(item => keyField(item) -> valueField(item)).toMap
      }
      .fold(Map.empty[String, String]) { (accMap, rowMap) =>
        accMap ++ rowMap // Combina los mapas de todas las filas
      }
      .compile
      .lastOrError
  }

  /**
   * Versión simplificada para géneros
   */
  def analyzeGenres(filePath: Path, separator: Char = ';'): IO[Map[String, Int]] = {
    import models.Genres
    analyzeJsonField[Genres](filePath, "genres", _.name, separator)
  }

  /**
   * Versión simplificada para crew (por job)
   */
  def analyzeCrewByJob(filePath: Path, separator: Char = ';'): IO[Map[String, Int]] = {
    import models.Crew
    analyzeJsonField[Crew](filePath, "crew", _.job, separator)
  }

  /**
   * Versión simplificada para crew (por department)
   */
  def analyzeCrewByDepartment(filePath: Path, separator: Char = ';'): IO[Map[String, Int]] = {
    import models.Crew
    analyzeJsonField[Crew](filePath, "crew", _.department, separator)
  }

  /**
   * Versión simplificada para cast (por actor)
   */
  def analyzeCastByName(filePath: Path, separator: Char = ';'): IO[Map[String, Int]] = {
    import models.Cast
    analyzeJsonField[Cast](filePath, "cast", _.name, separator)
  }

  def analisisKeyWords(filePath: Path, separator: Char = ';'): IO[Map[String, Int]] = {
    import models.Keywords
    analyzeJsonField[Keywords](filePath, "keywords", _.name, separator)
  }

  def analisisSpokenLenguaje(filePath: Path, separator: Char = ';'): IO[Map[String, Int]] = {
    import models.Spoken_Languages
    // Esta función cuenta cuántas veces aparece "English", "Spanish", etc.
    analyzeJsonField[Spoken_Languages](filePath, "spoken_languages", _.name, separator)
  }
}