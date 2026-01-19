# Reporte de Análisis de Datos de Películas (Pi Movies)

Este proyecto realiza un análisis exploratorio y descriptivo sobre un dataset de películas, aplicando principios de programación funcional.

## 1. Tablas de Datos

#  Dataset de Películas

## Estructura de la Tabla de Datos

| Columna | Tipo de Dato | Descripción |
| :--- | :--- | :--- |
| **adult** | Boolean | Indica si la película está dirigida solo hacia adultos. |
| **belongs_to_collection** | JSON | Información de la colección: Id, Name, Poster_path, Backdrop_path. |
| **budget** | Double | El presupuesto de la película. |
| **genres** | JSON | Géneros de la película: id, Name. |
| **homepage** | String | Enlace a la página oficial del estudio o película. |
| **id** | Int | Identificador único de la película. |
| **imdb_id** | String | Identificador único asignado en la base de datos de IMDb. |
| **original_language** | String | Idioma original de la película. |
| **original_title** | String | Título original de la película. |
| **overview** | String | Sinopsis o resumen de la trama. |
| **popularity** | Double | Índice de popularidad de la película. |
| **poster_path** | String | Ruta y nombre de la imagen del póster en el servidor de alojamiento. |
| **production_companies** | JSON | Lista de compañías productoras involucradas (Name, Id). |
| **production_countries** | JSON | Países de producción o financiación (iso_3166_1, name). |
| **release_date** | Date | Fecha de lanzamiento oficial de la película. |
| **revenue** | Double | Ganancia total obtenida por la película. |
| **runtime** | Double | Duración de la película en minutos. |
| **spoken_languages** | JSON | Idiomas hablados en el film (iso_639_1, name). |
| **status** | String | Condición actual de la película (ej. Rumored, Released, Post Production). |
| **tagline** | String | Slogan o frase publicitaria. |
| **title** | String | Nombre comercial de la película. |
| **video** | Boolean | Indica si existe un video promocional o tráiler disponible. |
| **vote_average** | Double | Promedio de las votaciones recibidas. |
| **vote_count** | Int | Cantidad total de votos registrados para la película. |
| **keywords** | JSON | Palabras clave que influyen en el tono y clasificación (id, name). |
| **cast** | JSON | Reparto de actores y sus personajes (cast_id, character, credit_id, gender, name, profile_path). |
| **crew** | JSON | Personal detrás de cámaras (name, job, department, id, gender, credit_id). |
| **rating** | JSON | Detalles de calificación por usuario (userID, rating, timestamp). |

## Observaciones sobre Formatos
* **Columnas JSON:** Estas columnas contienen estructuras anidadas u objetos que requieren ser decodificados o procesados para extraer sus atributos internos.
* **Identificadores:** El campo `id` actúa como la clave primaria para la integración de datos entre diferentes tablas o fuentes.

## 2. Lectura y Limpieza de Datos
``` Scala
import cats.effect.{IO, IOApp}
import fs2.io.file.{Files, Path}
import fs2.{Stream, text}
import fs2.data.csv.lenient.attemptDecodeUsingHeaders
import fs2.data.csv.*
import fs2.data.csv.generic.semiauto.*
import cats.implicits.*
// 5.1 Estructura de Datos
case class Movie(
                  adult: Boolean,
                  revenue: Double,
                  budget: Double,
                  vote_count: Int,
                  id: Int,
                  original_language: String,
                  original_title: String
                )

object MovieAnalysis extends IOApp.Simple {

  // 5.2 y 5.5 Lectura y Limpieza (Decodificadores seguros)
  implicit val trimIntDecoder: CellDecoder[Int] =
    CellDecoder.stringDecoder.map(_.trim).map(s => s.toIntOption.getOrElse(0))

  implicit val trimDoubleDecoder: CellDecoder[Double] =
    CellDecoder.stringDecoder.map(_.trim).map(s => s.toDoubleOption.getOrElse(0.0))

  implicit val trimBoolDecoder: CellDecoder[Boolean] =
    CellDecoder.stringDecoder.map(_.trim.toLowerCase).map {
      case "true" | "1" | "t" | "true.0" => true
      case _ => false
    }

  implicit val trimStringDecoder: CellDecoder[String] =
    CellDecoder.stringDecoder.map(_.trim)

  implicit val movieDecoder: CsvRowDecoder[Movie, String] = deriveCsvRowDecoder

  val filePath: Path = Path("src/main/resources/data/pi_movies_complete (3) - copia.csv")

  def run: IO[Unit] = {
    // 5.2 Lectura del archivo
    val lecturaCSV: IO[List[Either[CsvException, Movie]]] = Files[IO]
      .readAll(filePath)
      .through(text.utf8.decode)
      .through(attemptDecodeUsingHeaders[Movie](separator = ';'))
      .compile
      .toList

    lecturaCSV.flatMap { resultados =>
      // 5.5 Limpieza de datos (Separar filas válidas de inválidas)
      val movies: List[Movie] = resultados.collect { case Right(m) => m }
      val totalErrores:Int = resultados.collect { case Left(e) => e }.length

      if (movies.isEmpty) {
        IO.println(" ERROR: No se pudieron cargar datos válidos.")
      } else {
        val n:Int = movies.length

        // 5.3 Análisis Numérico (Estadísticas básicas)
        val avgRevenue:Double = movies.map(_.revenue).sum / n.toDouble
        val moviesAboveAvgCount:Double = movies.count(_.revenue > avgRevenue)
        val avgBudget:Double = movies.map(_.budget).sum / n.toDouble
        val sumSquaredDiff:Double = movies.map(m => math.pow(m.budget - avgBudget, 2)).sum
        val stdDevBudget:Double = math.sqrt(sumSquaredDiff / n)
        // 5.4 Análisis de Texto (Distribución de frecuencia de idiomas)
        // Agrupamos por idioma y contamos cuántos hay de cada uno
        val distribucionIdiomas: List[(String, Int)] = movies
          .groupBy(_.original_language)
          .map { case (idioma, lista) => idioma -> lista.length }
          .toList
          .sortBy(-_._2) // Ordenamos de mayor a menor frecuencia
          .take(5)       // Tomamos el top 5 para mostrar

        IO.println("       REPORTE DE ANÁLISIS DE PELÍCULAS      ") >>
          IO.println(s"1. LIMPIEZA DE DATOS ") >>
          IO.println(s"   - Filas procesadas correctamente: $n") >>
          IO.println(s"   - Filas eliminadas por errores:   $totalErrores") >>
          IO.println("--------------------------------------------") >>
          IO.println(s"2. ESTADÍSTICAS NUMÉRICAS ") >>
          IO.println(f"   - Promedio de Ingresos:           $$$avgRevenue%,.2f") >>
          IO.println(s"   - Películas sobre el promedio:    $moviesAboveAvgCount") >>
          IO.println(f"   - Desv. Estándar Presupuesto:     $$$stdDevBudget%,.2f") >>
          IO.println("--------------------------------------------") >>
          IO.println(s"3. FRECUENCIA DE IDIOMAS ") >>
          distribucionIdiomas.traverse { case (idioma, cantidad) =>
            IO.println(f"   - Idioma '$idioma': $cantidad%5d películas")
          } >>
          IO.println("=" * 20)
      }
    }
  }
}
```
Para garantizar la integridad de los datos al leer el archivo CSV, se implementaron las siguientes estrategias de limpieza:

* **Limpieza de Espacios:** Se aplica `.trim` a las cadenas de texto antes de la conversión numérica para eliminar espacios en blanco accidentales (ej. convertir `" 100 "` a `100`).
* **Filtrado de Filas Corruptas:** Se utiliza `attemptDecodeUsingHeaders` para capturar errores de parseo y descartar filas que no cumplan con la estructura esperada.

## 3. Análisis Realizado

### Análisis Numérico (Estadísticas Básicas)
Se calcularon las siguientes métricas sobre las columnas `revenue` y `budget`:
* **Promedio (Mean):** Calculado para el presupuesto y la recaudación.
```scala
val avgRevenue:Double = movies.map(_.revenue).sum / n.toDouble
```
* **Conteo Superior al Promedio:** Cantidad de películas cuya recaudación supera la media global.
```scala
val moviesAboveAvgCount:Double = movies.count(_.revenue > avgRevenue)
```
* **Desviación Estándar:** Calculada para la columna `budget` utilizando la raíz cuadrada de la suma de las diferencias al cuadrado dividida por N.
```scala
val sumSquaredDiff:Double = movies.map(m => math.pow(m.budget - avgBudget, 2)).sum
val stdDevBudget:Double = math.sqrt(sumSquaredDiff / n)
```
### Análisis de Texto (Distribución de Frecuencia)
Se analizó la columna **`original_language`** (excluyendo columnas JSON complejas como *genres* o *cast* para esta etapa).
* **Metodologia:** Agrupamiento (`groupBy`) por idioma y conteo de ocurrencias.
* **Resultado:** Top 5 de idiomas más frecuentes en el dataset.