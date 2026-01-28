# PROYECTO INTEGRADOR PRACTICCUM1.1

## DOCUMENTACION:

## Package data:

## LecturaCSV

importaciones

```scala
import cats.effect.IO
import fs2.io.file.{Files, Path}
import fs2.data.csv.lenient.attemptDecodeUsingHeaders
import fs2.data.csv.generic.semiauto.*
import models.Movie
import utilities.CSVDecoder.*
```

- `import cats.effect.IO` Representa una operación pendiente que puede tener efectos secundarios (como leer un archivo). En lugar de que tu función devuelva los datos directamente, devuelve un IO que promete entregarlos.

- `import fs2.io.file.{Files, Path}` Componentes de la librería FS2 (Functional Streams for Scala) para manejo de archivos.<br>
  - `Files` Proporciona las herramientas para leer y escribir en el sistema de archivos de forma asíncrona y mediante streams (flujos de datos).<br>
  - `Path`: Es un objeto que representa la ubicación del archivo en tu disco duro (ej: data/peliculas.csv)<br>

- `import fs2.data.csv.lenient.attemptDecodeUsingHeaders`Un decodificador de CSV con tolerancia a errores.
  - `lenient`: Si encuentra una fila con un error, no detiene todo el programa; simplemente marca esa fila como un error y sigue con la siguiente.<br>
  - `attemptDecodeUsingHeaders`: Usa la primera fila del archivo CSV como guía por ejemplo la columna se llama "title" buscará meter ese dato en el campo title de tu clase.<br>

- `import fs2.data.csv.generic.semiauto.*`:Permite que Scala genere automáticamente el código necesario para convertir una fila de texto del CSV a tu objeto Movie esto permite evitar que escribir a mano cómo se mapea cada columna una por una. El asterisco (\*) trae funciones como deriveCsvRowDecoder

- `import models.Movie` es la importacion del modelo de pelicula del proyecto:

```scala
case class Movie(
                        id: Int,
                        imdb_id: String = "",
                        title: String = "",
                        original_title: String = "",
                        original_language: String = "",
                        overview: String = "",
                        tagline: String = "",
                        adult: Boolean = false,
                        video: Boolean = false,
                        status: String = "",
                        release_date: String = "",
                        budget: Double = 0.0,
                        revenue: Double = 0.0,
                        runtime: Double = 0.0,
                        popularity: Double = 0.0,
                        vote_average: Double = 0.0,
                        vote_count: Double = 0,
                        homepage: String = "",
                        poster_path: String = "",
                        belongs_to_collection: String = "[]",
                        genres: String = "[]",
                        production_companies: String = "[]",
                        production_countries: String = "[]",
                        spoken_languages: String = "[]",
                        keywords: String = "[]",
                        cast: String = "[]",
                        crew: String = "[]"
                )
```

¿Porq qué definir explicitamente el como debe contener los datos? <br>
Sin el valor predefinido, si el CSV tiene una fila donde falta el campo por ejemplo crew, la librería fs2-data-csv lanzará una excepción como estamos usando attemptDecodeUsingHeaders, esa película se convertirá en un Left(error), perdiendo la información de esa fila.<br>
Con el valor "[]": El programa detecta el hueco, le asigna el "JSON vacío" ([]) y te entrega un Right(Movie) maneniendo la película en la lista de resultados.

- `import utilities.CSVDecoder.*` esta importacion son los decoders usados en el packagee creado llamado "utilities"<br>

### Contenido de la exportacion (CSVDecoder):<br>

```scala
import fs2.data.csv.CellDecoder
object CSVDecoder {

  implicit val trimIntDecoder: CellDecoder[Int] =
    CellDecoder.stringDecoder.map(_.trim).map { s =>
      s.toIntOption.getOrElse(0)
    }

  implicit val trimDoubleDecoder: CellDecoder[Double] =
    CellDecoder.stringDecoder.map(_.trim).map { s =>
      s.toDoubleOption.getOrElse(0.0)
    }

  implicit val trimBoolDecoder: CellDecoder[Boolean] =
    CellDecoder.stringDecoder.map(_.trim.toLowerCase).map {
      case "true" | "1" | "t" | "true.0" => true
      case _ => false
    }

  implicit val trimStringDecoder: CellDecoder[String] =
    CellDecoder.stringDecoder.map(_.trim)
}
```

este objeto ya que define como limpiar y transformar los datos antes de guardarlos

`implicit` implica que Scala pasa automaticamente este traductor a las funciones que lo necesitan como por ejemplo `deriveCsvRowDecoder`<br>
`CellDecoder[T]` es un traductor de la libreria fs2 que sabe como convertir un trozo del CSV a un tipo de dato `T` (int,double,boolean, etc)<br>

```scala
implicit val trimIntDecoder: CellDecoder[Int] =
    CellDecoder.stringDecoder.map(_.trim).map { s =>
      s.toIntOption.getOrElse(0)
    }
```

`CellDecocer.StringDecoder` toma el contenido de la celda y lo lee como una cadena de texto<br>
`.map(_.trim)` crea una lista quitando los espacios en blanco inncesarios<br>
`.map{s => s.toIntOption.getOrElse(0)}` s representa el string anteriormente mapeado y lo convierte a un tipo de dato int, y si lo puede convertir a un tipo de dato int devuelve Some que representa el valor existente, el getOrElse saca el número existente por ejemplo si hay Some(125) saca el valor 125 y si no lo encuntra (caso "None" por el tipo de dato Option[]) devuelve un 0<br>

```scala
implicit val trimDoubleDecoder: CellDecoder[Double] =
    CellDecoder.stringDecoder.map(_.trim).map { s =>
      s.toDoubleOption.getOrElse(0.0)
    }
```

`CellDecocer.StringDecoder` toma el contenido de la celda y lo lee como una cadena de texto<br>
`.map(_.trim)` crea una lista quitando los espacios en blanco inncesarios<br>
`.map { s => s.toDoubleOption.getOrElse(0.0)` s representa el string anteriormente mapeado y lo convierte a un tipo de dato double, y si lo puede convertir a un tipo de dato double devuelve Some que representa el valor existente, el getOrElse saca el número existente por ejemplo si hay Some(125) saca el valor 125 y si no lo encuntra (caso "None" por el tipo de dato Option[]) devuelve un 0.0<br>

```scala
implicit val trimBoolDecoder: CellDecoder[Boolean] =
    CellDecoder.stringDecoder.map(_.trim.toLowerCase).map {
      case "true" | "1" | "t" | "true.0" => true
      case _ => false
    }
```

`CellDecocer.StringDecoder` toma el contenido de la celda y lo lee como una cadena de texto<br>
`map(_.trim.toLowerCase)` crea una lista aparte eliminando los espacios en blanco y lo transforma en minúsculas para estandarizar<br>

```scala
.map {
      case "true" | "1" | "t" | "true.0" => true
      case _ => false
    }
```

mapea desde la lista estandarizada a minusculas y verifica varios casos donde sea true, caso contrario de no encontrar ningun caso anterior devuelve un false.<br>

```scala
implicit val trimStringDecoder: CellDecoder[String] =
    CellDecoder.stringDecoder.map(_.trim)
```

`CellDecoder.stringDecoder.map(_.trim)` lee desde el texto CSV y elimina los espacios en blanco<br>

```scala
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
```

`filePath`: La ruta del archivo en tu computadora.

`separator`: El carácter que separa las columnas

`IO[...]`: Indica que esto es una operación de entrada/salida (leer disco) que puede fallar o tardar.

`List[Either[String, Movie]]`: El resultado será una lista. Cada elemento puede ser un Right(Movie) si se leyó bien, o un Left(String) con un mensaje si esa fila específica falló.

```scala
import fs2.data.csv.CsvRowDecoder
implicit val movieDecoder: CsvRowDecoder[Movie, String] = deriveCsvRowDecoder[Movie]
```

`CsvRowDecoder[Movie, String]`: toma una fila de texto y reparte cada CellDecoder correspondiente y recoge todos los resultados en el **case class movie**, el dato Movie es el tipo de destino y el String es la cabecera que indica la columna en que se va a trabajar<br>
`deriveCsvRowDecoder[Movie]`: Esta es una función "mágica" que mira tu case class Movie y mira tu CSVDecoder (donde están los trimIntDecoder, etc.) y construye automáticamente un plano para traducir una fila completa del CSV a una película.<br>
`Files[IO].readAll(filePath)` Los datos entran al programa como bytes es altamente eficiente porque utiliza I/O no bloqueante y lee el archivo en pedazos, evitando saturar la memoria RAM si el CSV pesa gigabytes.
`.through(fs2.text.utf8.decode)` convierte los bytes crudos a texto
`.through(attemptDecodeUsingHeaders[Movie](separator = separator)`Usa el implicit movieDecoder que se definió arriba para saber cómo mapear cada columna al campo correcto de la case class, attempt: Al ser "intentar", el Stream cambia su tipo de salida. Ahora emite objetos de tipo Either[CsvException, Movie]

```scala
.map {
        case Right(movie) => Right(movie)
        case Left(error) => Left(error.getMessage)
      }
```

mapea el obejto Either anteriormente emitido el dato Either[] tiene 2 casos right y left normalmente el caso right es el caso que queremos y el left es el error en este caso el caso right devuelve el obejto Movie y el left el error

```scala
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
```

`fieldName: String` Define el identificador de la columna sobre la cual se ejecutará la lógica de negocio.<br>
`validator: String => Boolean: `Recibe un predicado (una función de orden superior) que determina la validez de los datos.<br>
`readAll(filePath)`Abre el archivo y emite un flujo de bytes binarios.<br>
`.through(fs2.text.utf8.decode)`Transforma esos bytes en caracteres de texto. Es el mismo inicio que en la función anterior para asegurar que el archivo sea legible.<br>
`.through(attemptDecodeUsingHeaders[Map[String, String]](separator = separator))`
`.collect { case Right(row) => row }`collect actúa como un filtro inteligente si la decodificación de la fila fue exitosa deja pasar el mapa de datos si la fila estaba corrupta o mal formateada la descarta silenciosamente y no la incluye en el conteo.
`.map(row => row.getOrElse(fieldName, ""))`De todo el mapa (que tiene todas las columnas), esta línea extrae solo una columna (la que definiste en fieldName) y getOrElse para el caso de que la columna no existe en esa fila, devuelve un texto vacío ("") para evitar que el programa falle.<br>
`.filter(validator)`El flujo solo permite pasar a los elementos que cumplan con la condición del validator<br>
`.compile`Prepara el flujo para su ejecución final.
`.count`incrementa un contador cada vez que una fila logra pasar todos los filtros anteriores

```scala
def readCsvAsMap(filePath: Path, separator: Char = ';'): IO[List[Map[String, String]]] = {
    Files[IO]
      .readAll(filePath)
      .through(fs2.text.utf8.decode)
      .through(attemptDecodeUsingHeaders[Map[String, String]](separator = separator))
      .collect { case Right(row) => row }
      .compile
      .toList
  }
```

`def readCsvAsMap(filePath: Path, separator: Char = ';'): IO[List[Map[String, String]]]`<be>
Define un proceso IO que resultará en una lista de diccionarios.<br>
Cada fila del CSV será un Map donde la clave es el nombre de la columna y el valor es el contenido de la celda, ambos tratados como texto (String)<br>
`Files[IO].readAll(filePath)`Extrae los datos bit a bit, emitiendo un flujo de bytes (Stream[IO, Byte]). Al igual que en las otras funciones, esto garantiza que el uso de memoria RAM sea mínimo, incluso con archivos de gran volumen.<br>
`.through(fs2.text.utf8.decode)`Actúa como un transductor que interpreta los bytes y los agrupa en caracteres bajo el estándar UTF-8.<br>
`.through(attemptDecodeUsingHeaders[Map[String, String]](separator = separator))`Utiliza la primera línea del archivo para registrar los nombres de las columnas, convierte cada línea subsiguiente en un objeto Map y emite un tipo Either[CsvException, Map[String, String]]. Si una fila no tiene el número correcto de columnas según el separador indicado, generará un error (Left).<br>
`.collect { case Right(row) => row }`Utiliza una "limpieza" para quedarse únicamente con los resultados exitosos<br>
`.compile`Prepara el motor de FS2 para recorrer todo el archivo desde la primera hasta la última línea<br>
`.toList`convierte el resultado en una lista<br>

## LecturaJSON

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
import java.time.format.DateTimeFormatter
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

```scala
import cats.effect.IO
import fs2.io.file.{Files, Path}
import io.circe.Decoder
import utilities.Parsear_JSON
import models.*
import fs2.data.csv.lenient.attemptDecodeUsingHeaders
import io.circe.generic.auto.deriveDecoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try
```

`import cats.effect.IO`deja que las tareas peligrosas (como leer archivos) ocurran sin control, las envuelve en un objeto IO. Esto asegura que si algo falla, el programa no "explote" de golpe, sino que maneje el error ordenadamente<br>
`import fs2.io.file.{Files, Path}`Files es la banda transportadora. Permite que los datos fluyan poco a poco (streaming) desde el disco duro sin llenar la memoria RAM.<br>
Path es simplemente la dirección postal del archivo.<br>
`import io.circe.Decoder`Le dice al programa cómo debe "entender" un texto en formato JSON para convertirlo en una pieza de información útil en Scala.
`import models.*`importa todos los modelos definidos segun el dataset
`import fs2.data.csv.lenient.attemptDecodeUsingHeaders`Es el separador de columnas toma las líneas del archivo CSV y usando la primera fila (cabecera) como guía organiza la información la palabra lenient significa que si el archivo tiene pequeños errores de formato, intentará seguir trabajando en lugar de detenerse<br>
`import io.circe.generic.auto.deriveDecoder`Evita tener que escribir manualmente cómo se traduce cada campo; la máquina mira tu modelo de datos y deduce las reglas de traducción por sí sola<br>
`import java.time.LocalDate`, `import java.time.format.DateTimeFormatter`Son el reloj y el calendario. Permiten que el programa entienda que "1995-12-15" no es solo un texto, sino una fecha real, permitiéndote saber qué película es más antigua o más reciente<br>
`import scala.util.Try`Se usa cuando algo que podría fallar, si falla, Try captura el error y te permite dar una respuesta alternativa sin detener la fábrica<br>

```scala
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
```

estas tres clases sirven para empaquetar los resultados después de haber procesado miles de filas

- RatingsStats: El Reporte de Calificaciones
  - Se usa para agrupar toda la estadística de los usuarios que han puntuado películas.

  - Uso: En lugar de que tu función analisisRatings te devuelva 5 números sueltos (lo cual sería confuso), te devuelve este "paquete" donde ya tienes calculado el promedio y los totales. Es como la hoja de resultados de un examen.

- FechaStats: El Control de Calidad
  - Se usa para entender qué tan "limpio" está tu archivo respecto a las fechas.

  - Uso: Te permite saber cuántas fechas se pudieron entender y cuántas estaban mal escritas. Además, guarda una pequeña lista (ejemplosFechasParseadas) para que puedas ver visualmente cómo quedó la transformación de, por ejemplo, "1995-12-05" a "1995/12/05".

- AnalisisGeneral: El Top Ranking
  - Se usa para campos repetitivos como Géneros, Actores o Palabras Clave.

  - Uso: Sirve para mostrar los resultados de conteo. Te dice cuántos elementos únicos hay (ej: 18 géneros distintos) y cuáles son los más populares (el topItems).

```scala
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
```

---

```scala
def analyzeJsonField[T](
                           filePath: Path,
                           fieldName: String,
                           extractKey: T => String,
                           separator: Char = ';'
                         )(implicit decoder: Decoder[List[T]]): IO[Map[String, Int]]
```

Define una función genérica donde [T] es el tipo de objeto que vive dentro del JSON (como Genre o Cast)<br>
`extractKey: T => String` Es una función que le dice al código qué campo del JSON queremos contar (por ejemplo, el nombre del género)<br>
`(implicit decoder: Decoder[List[T]])`Un Decoder es un objeto que sabe cómo transformar un texto bruto (en formato JSON) en un objeto real de Scala (como una List[Genre]), la función es genérica ([T]), el código aun no sabe de antemano si vas a procesar géneros, actores o países<br>

`Files[IO].readAll(filePath)`: Abre un flujo de lectura hacia el archivo. Emite bytes crudos. No carga el archivo en memoria, lo lee por pedazos (chunks).

`.through(fs2.text.utf8.decode)`: Transforma esos bytes en caracteres de texto legibles usando el estándar UTF-8.

`.through(attemptDecodeUsingHeaders[Map[String, String]](...))`: Interpreta el texto como un CSV. Usa la primera línea como cabecera y convierte cada fila en un Mapa (clave -> valor) de strings. El attempt hace que si una fila está mal, no rompa el programa, sino que devuelva un error.

`.collect { case Right(row) => row }`: Filtra el flujo. Solo deja pasar las filas que se decodificaron correctamente (Right) y descarta las que dieron error (Left).

2. Transformación de la Celda JSON
   `.map { row => ... }`: Por cada fila válida, ejecuta lo siguiente:

`val jsonRaw = row.getOrElse(fieldName, "[]")`: Extrae el texto de la columna que nos interesa (ej. "genres"). Si no existe, asume una lista vacía.

`val parsed = Parsear_JSON.parseJsonField[T](jsonRaw)`: Convierte ese texto JSON en una Lista de objetos Scala (gracias al Decoder que vimos antes).

`parsed.groupMapReduce(extractKey)(_ => 1)(_ + _)`: Crea un pequeño resumen de esa fila.

Ejemplo: Si la película es de "Acción" y "Comedia", genera: Map("Action" -> 1, "Comedy" -> 1).

3. Agregación Global (El Acumulador)
   `.fold(Map.empty[String, Int]) { (acc, rowMap) => ... }`: Esta es la parte donde se "suman" todas las películas del archivo. Empieza con un mapa vacío (acc) y va recibiendo los mapas de cada fila (rowMap).

`rowMap.foldLeft(acc) { ... }`: Recorre el resumen de la película actual y lo mezcla con el total acumulado que llevamos.

`currentAcc.updated(k, currentAcc.getOrElse(k, 0) + v)`: Si el género ya estaba en el total, le suma el valor nuevo; si no estaba, lo crea.

4. Finalización del Proceso
   `.compile`: Detiene la definición del flujo y prepara el motor de ejecución.

`.lastOrError`: Como el proceso de fold termina entregando un único resultado (el Mapa final con todos los conteos del archivo), esta línea extrae ese resultado si por alguna razón el flujo no produjo nada (archivo vacío), lanza una excepción.

```scala
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
```

analyzeGenres:

- Busca la columna "genres", la convierte en objetos de tipo Genres y cuenta cuántas películas pertenecen a cada Nombre de Género (ej: Terror, Comedia).

analyzeCrewByJob:

- Entra en la columna "crew" (equipo técnico) y cuenta cuántas personas hay por cada Puesto de trabajo (ej: Director, Editor, Productor).

analyzeCrewByDepartment:

- Similar a la anterior, pero agrupa por Departamento (ej: Cámara, Sonido, Arte) en lugar de puestos individuales.

analyzeCastByName:

- Analiza la columna "cast" (actores) y cuenta cuántas veces aparece cada Nombre de Actor en todo tu catálogo de películas.

analisisKeyWords:

- Busca en la columna "keywords" y genera una estadística de las Etiquetas o Palabras clave más usadas (ej: "basado en novela", "superhéroes").

analisisSpokenLenguaje:

- Revisa la columna "spoken_languages" para contar qué Idiomas son los más frecuentes en las películas procesadas.

Análisis de Objetos Únicos y Otros
analisisColecciones:

- Diferencia clave: Usa analyzeJsonFieldSingle porque una película solo puede pertenecer a una colección (ej: Saga Harry Potter). Cuenta cuántas películas tiene cada saga.

analisisCompanias:

- Cuenta cuántas películas ha producido cada Empresa productora (ej: Warner Bros, Pixar) basándose en la columna "production_companies".

analisisPaises:

- Analiza la columna "production_countries" para saber qué Países son los que más cine producen según tus datos.

```scala
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
```

`.fold(0L)(_ + _)` El fold empieza en cero (0L) y va sumando cada número que llega al total acumulado<br>

```scala
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
```

`.compile`: Prepara el flujo para terminar<br>
`.toList`: Detiene el flujo y guarda todo lo que pasó en una lista física en la memoria RAM<br>
`.map(...)`Como estamos dentro de un IO, este .map no es para transformar la lista directamente, sino para cuando tenga la lista luego de que convierta la lista<br>
`.map(_.flatten.distinct.length.toLong)`<br>
`flatten`: Como tenías una estructura de "Lista de Listas" (porque cada película traía su propia lista de usuarios), flatten las aplasta todas en una sola lista gigante de IDs <br>
`distinct`: Este es el paso más importante. Revisa toda la lista gigante y elimina los duplicados. Si el "Usuario 1" calificó 10 películas diferentes, aquí solo quedará una vez<br>
`length`: Cuenta cuántos elementos quedaron en esa lista limpia. Ese es tu número de usuarios únicos<br>
`toLong`: Convierte ese conteo a un número de tipo Long para evitar errores si el número de usuarios es extremadamente grande<br>

```scala
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
```

`def parsearFechaEstreno(fechaStr: String): Option[String]`Define la función. Recibe un texto (String) y devuelve un Option devuelve Some(fecha) si se parseo la fecha o None.<br>
`val targetFormatter = ...("yyyy/MM/dd")` define la regla de estandarizar la fecha yyyy/mm/dd<br>
`val formatters = List(..)` se define como podria estar las fechas en el dataaset
`if (fechaStr == null || fechaStr.trim.isEmpty || fechaStr.equalsIgnoreCase("null")) return None` verifica si esta vacia o si esta "null" la celda<br>
`val cleanStr = fechaStr.trim` limpia los espacios de la fecha tipo texto
`formatters.foldLeft[Option[LocalDate]](None) { (result, formatter) => ... }`: Esta es la parte más inteligente. La función recorre la lista de formatos uno por uno:<br>

- `result.orElse { ... }`: Significa: "Si ya logré traducir la fecha con un formato anterior, quédate con ese. Si no, intenta con el siguiente".

- `Try(LocalDate.parse(cleanStr, formatter)).toOption`: Intenta convertir el texto a una fecha real usando el formato actual. Si falla (porque el formato no coincide), en lugar de romperse, devuelve None y sigue probando.

- `orElse(if (cleanStr.matches("\\d{4}")) Try(LocalDate.of(cleanStr.toInt, 1, 1)).toOption else None` Si el texto son solo 4 números (como "1995"), asume que es el 1 de enero de ese año.

`.map(_.format(targetFormatter))` mapea cada fecha valida como la definimos

```scala
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
```

`.map { row => ... }`: Por cada película, hace lo siguiente:

- `val fechaOriginal = row.getOrElse("release_date", "")`: Extrae el texto de la columna de fecha.

- `val fechaParseada = parsearFechaEstreno(fechaOriginal)`: Llama a la función "traductora" que vimos antes. El resultado es un Option[String] (puede ser Some("fecha") o None).

- `(fechaOriginal, fechaParseada)`: Crea una Tupla (una pareja de datos) con el valor sucio y el valor limpio para compararlos luego<br>

`.compile.toList`: Detiene el flujo y mete todas esas parejas (tuplas) en una lista en memoria para poder contarlas.

- `.map { fechas => ... }`: Aquí se calculan los resultados finales:
  - `val total = fechas.length.toLong`: Cuenta cuántas películas hay en total.

  - `val validas = fechas.count(_._2.isDefined).toLong`:
    - `_._2`: Accede al segundo elemento de la tupla (la fecha parseada).

    - `.isDefined`: Es un método de los Option. Devuelve true si el Option contiene algo (Some) y false si está vacío (None). Básicamente, cuenta cuántas fechas se pudieron arreglar.

`total - validas:` Calcula cuántas fechas se perdieron o eran nulas<br>

`FechaStats(total, validas, total - validas, fechas.collect { case (o, Some(p)) => (o, p) }.take(10))`

1. `total`
   Qué es: El número total de filas procesadas.

Función: Indica el tamaño completo de tu dataset (ej: 45,000 películas).

2. `validas`
   Qué es: El conteo de cuántas veces el Option fue Some.

Función: Indica cuántas fechas estaban bien escritas o pudieron ser corregidas por tu "traductor".

3. `total - validas`
   Qué es: Una resta simple entre el total y las válidas.

Función: Calcula automáticamente las fechas inválidas o nulas. Es el número de "errores" que se encontraron.

- `fechas.collect { case (o, Some(p)) => (o, p) }.take(10)`
  - `.collect { case (o, Some(p)) => (o, p) }`:Filtra la lista: "Dame solo las que sí tienen una fecha parseada".Limpia el dato: Convierte un (String, Option[String]) en un simple (String, String). Quita la "envoltura" Some para que el texto quede limpio.

  - `.take(10)`Solo toma los primeros 10 para que sirvan de muestra

```scala
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
```

`total <- contarTotalRatings(filePath, separator)`: Llama a tu otra función para obtener el número total de votos en todo el archivo.

`unicos <- contarUsuariosUnicos(filePath, separator)`: Llama a la función que cuenta cuántas personas distintas votaron (quitando duplicados).

`peliculasConRatings <- Files[IO].readAll(filePath)...`: Inicia un nuevo flujo para contar cuántas películas tienen al menos una calificación.

.map { row => ... }: Aquí aplicas una lógica de filtro rápido:

`val json = row.getOrElse("ratings", "[]")`: Obtienes el texto de la columna ratings.

`if (json.length > 5 && json != "[]") 1L else 0L`: Esta es una optimización en lugar de parsear el JSON solo vemos si hay contenido en el texto si mide más de 5 caracteres y no es [], devuelves un 1 (película con rating), si no, un 0.

`.compile.fold(0L)(_ + _)`: Suma todos esos 1 para dar el total de películas calificadas.

Todo lo que genere yield quedará automáticamente envuelto en un IO. Por eso la función devuelve un IO[RatingsStats].

se ensambla el **case class RatingsStats** final con los datos recolectados:

- `totalRatings = total`: Asigna el gran total de votos.

- `usuariosUnicos = unicos`: Asigna el conteo de personas diferentes.

- `peliculasConRatings = peliculasConRatings`: El número de películas que no están en "cero" votos.

- `promedioRatingsPorPelicula = ...`:

- Hace una división: `Total de votos / Películas calificadas`
  - `total.toDouble`: Convierte el número a decimal para que la división sea exacta (ej: 4.5) y no redondee a un entero.

  - `if (peliculasConRatings > 0)`: Es una protección de seguridad para evitar que el programa explote si intentas dividir por cero (en caso de un archivo vacío).

## Package utilities

### Estadistico

```scala
package utilities

import models.NUmEstadisticas
import scala.math._

object Estadistico {

  /**
   * Calcula estadísticas descriptivas completas para una lista de valores
   */
  def calculateStats(values: List[Double]): NumEstadisticas = {
    if (values.isEmpty) {
      return NumEstadisticas(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }

    val sorted = values.sorted
    val n = values.length
    val mean = values.sum / n.toDouble

    // Mediana
    val median = if (n % 2 == 0) {
      (sorted(n / 2 - 1) + sorted(n / 2)) / 2.0
    } else {
      sorted(n / 2)
    }

    // Desviación estándar
    val variance = values.map(v => pow(v - mean, 2)).sum / n
    val stdDev = sqrt(variance)

    // Cuartiles
    val q1 = sorted((n * 0.25).toInt)
    val q3 = sorted((n * 0.75).toInt)
    val iqr = q3 - q1

    NumEstadisticas(
      count = n,
      mean = mean,
      median = median,
      stdDev = stdDev,
      min = sorted.head,
      max = sorted.last,
      q1 = q1,
      q3 = q3,
      iqr = iqr
    )
  }

  /**
   * Calcula frecuencias para datos categóricos
   */
  def calculateFrequency[T](values: List[T]): Map[T, Int] = {
    values.groupBy(identity).view.mapValues(_.length).toMap
  }

  /**
   * Obtiene el top N más frecuentes
   */
  def topN[T](frequencies: Map[T, Int], n: Int): List[(T, Int)] = {
    frequencies.toList.sortBy(-_._2).take(n)
  }

  /**
   * Detecta outliers usando el método IQR
   */
  def detectOutliers(values: List[Double], multiplier: Double = 1.5): List[Double] = {
    val stats = calculateStats(values)
    val lowerBound = stats.q1 - (multiplier * stats.iqr)
    val upperBound = stats.q3 + (multiplier * stats.iqr)

    values.filter(v => v < lowerBound || v > upperBound)
  }

  /**
   * Calcula correlación de Pearson entre dos listas
   */
  def pearsonCorrelation(x: List[Double], y: List[Double]): Double = {
    require(x.length == y.length && x.nonEmpty, "Las listas deben tener la misma longitud y no estar vacías")

    val n = x.length
    val meanX = x.sum / n
    val meanY = y.sum / n

    val numerator = x.zip(y).map { case (xi, yi) =>
      (xi - meanX) * (yi - meanY)
    }.sum

    val denomX = sqrt(x.map(xi => pow(xi - meanX, 2)).sum)
    val denomY = sqrt(y.map(yi => pow(yi - meanY, 2)).sum)

    if (denomX == 0.0 || denomY == 0.0) 0.0
    else numerator / (denomX * denomY)
  }
}
```

Preparación y Promedio
`val sorted = values.sorted`: Ordena los números de menor a mayor. Esto es indispensable para calcular la mediana y los cuartiles.

`val n = values.length`: Obtiene el tamaño de la muestra (cuántos números hay).

`val mean = values.sum / n.toDouble`: Calcula la Media (Promedio) sumando todos los valores y dividiendo por el total.

Cálculo de la Mediana
La mediana es el valor que está justo en medio de la lista ordenada.

`if (n % 2 == 0)`: Si hay un número par de elementos, la mediana es el promedio de los dos números centrales.

`else { sorted(n / 2) }`: Si el número es impar, toma directamente el valor central.

Dispersión (Varianza y Desviación)
Esto mide qué tan alejados están los números del promedio (qué tan "regados" están los datos).

`val variance = values.map(v => pow(v - mean, 2)).sum / n`: Calcula la varianza. Eleva al cuadrado la distancia de cada número respecto al promedio, los suma y los divide.

`val stdDev = sqrt(variance)`: La Desviación Estándar es la raíz cuadrada de la varianza. Es más fácil de interpretar porque está en la misma unidad que los datos originales (ej: dólares o minutos).

Cuartiles y Rango Intercuartílico (IQR)
Sirven para entender la distribución por tramos (25%, 50%, 75%).

`val q1 = sorted((n * 0.25).toInt)`: El Primer Cuartil (Q1) es el valor por debajo del cual está el 25% de los datos.

`val q3 = sorted((n * 0.75).toInt)`: El Tercer Cuartil (Q3) es el valor por debajo del cual está el 75% de los datos.

`val iqr = q3 - q1`: El Rango Intercuartílico es la distancia entre Q3 y Q1. Se usa mucho para detectar "Outliers" (datos atípicos o exagerados).

El Resultado:
Finalmente, la función empaqueta todo en un objeto organizado:

`count`: Total de elementos.

`min y max`: El primer y el último elemento de la lista ordenada (sorted.head y sorted.last).

`mean, median, stdDev, q1, q3, iqr`: Los valores que calculamos arriba.

```scala
   def calculateFrequency[T](values: List[T]): Map[T, Int] = {
    values.groupBy(identity).view.mapValues(_.length).toMap
  }

  /**
   * Obtiene el top N más frecuentes
   */
  def topN[T](frequencies: Map[T, Int], n: Int): List[(T, Int)] = {
    frequencies.toList.sortBy(-_._2).take(n)
  }

  /**
   * Detecta outliers usando el método IQR
   */
  def detectOutliers(values: List[Double], multiplier: Double = 1.5): List[Double] = {
    val stats = calculateStats(values)
    val lowerBound = stats.q1 - (multiplier * stats.iqr)
    val upperBound = stats.q3 + (multiplier * stats.iqr)

    values.filter(v => v < lowerBound || v > upperBound)
  }
```

`groupBy`: Agrupa los elementos de la lista basándose en un criterio.

`identity`: Es una función que retorna lo mismo que le entregamos. Si el elemento es "en", la clave será "en". <br>
Resultado: Crea un Map donde cada clave es el elemento único y el valor es una lista con todas sus repeticiones. <br>
Ejemplo: Si recibes List("en", "es", "en"), el resultado de esta parte es: Map("en" -> List("en", "en"), "es" -> List("es"))

`.view`
Es un paso de optimización de rendimiento, normalmente, al transformar un mapa, Scala crea uno nuevo inmediatamente en la memoria RAM al poner '.view' solo crea una ventana o 'vista' de lo que quiero hacer esto ahorra memoria, especialmente si hay miles de géneros o idiomas, porque no se crean objetos intermedios innecesarios.

`.mapValues(_.length)`
Es el paso de conteo.

- `mapValues`: Le indica a Scala que queremos mantener las claves (los idiomas) tal cual están, pero queremos transformar los valores (las listas de repetidos).

- `_.length`: Por cada grupo, cuenta cuántos elementos hay en esa lista siguiendo el ejemplo: > La lista List("en", "en") se convierte en el número 2 la lista List("es") se convierte en el número 1.<br>

`.toMap`
Es el paso de materialización como veníamos trabajando con una "vista" (view) para ser eficientes, este último paso le dice a Scala que le entegue el map real

```scala
def pearsonCorrelation(x: List[Double], y: List[Double]): Double = {
    require(x.length == y.length && x.nonEmpty, "Las listas deben tener la misma longitud y no estar vacías")

    val n = x.length
    val meanX = x.sum / n
    val meanY = y.sum / n

    val numerator = x.zip(y).map { case (xi, yi) =>
      (xi - meanX) * (yi - meanY)
    }.sum

    val denomX = sqrt(x.map(xi => pow(xi - meanX, 2)).sum)
    val denomY = sqrt(y.map(yi => pow(yi - meanY, 2)).sum)

    if (denomX == 0.0 || denomY == 0.0) 0.0
    else numerator / (denomX * denomY)
}
```

Formula de la correlacion

$$r = \frac{\sum_{i=1}^{n} (x_i - \bar{x})(y_i - \bar{y})}{\sqrt{\sum_{i=1}^{n} (x_i - \bar{x})^2} \cdot \sqrt{\sum_{i=1}^{n} (y_i - \bar{y})^2}}$$

$n$: Número de muestras.<br>
$x_i, y_i$:Valores individuales de cada lista.<br>
$\bar{x}, \bar{y}$:Medias aritméticas de las listas $x$ e $y$.<br>

---

### Analisis Movie

```scala
package utilities
import cats.effect.IO
import cats.implicits._
import models._
import utilities._

object AnalisisMovie {

  /**
   * Análisis completo de estadísticas numéricas de películas
   */
  def analyzeMovieStats(movies: List[Movie]): IO[Unit] = {
    if (movies.isEmpty) {
      IO.println("ERROR: No hay datos para analizar")
    } else {
      val n = movies.length

      // Calcular estadísticas INCLUYENDO ceros
      val revenueStats = Estadistico.calculateStats(movies.map(_.revenue))
      val budgetStats = Estadistico.calculateStats(movies.map(_.budget))
      val runtimeStats = Estadistico.calculateStats(movies.map(_.runtime).filter(_ > 0))
      val voteStats = Estadistico.calculateStats(movies.map(_.vote_average).filter(_ > 0))

      //Calcular estadísticas SIN ceros
      val revenueNonZero = movies.map(_.revenue).filter(_ > 0)
      val budgetNonZero = movies.map(_.budget).filter(_ > 0)

      val revenueStatsNonZero = Estadistico.calculateStats(revenueNonZero)
      val budgetStatsNonZero = Estadistico.calculateStats(budgetNonZero)

      // Contar películas con valores en 0
      val revenueZeros = movies.count(_.revenue == 0.0)
      val budgetZeros = movies.count(_.budget == 0.0)

      // Análisis de idiomas
      val languageFreq = Estadistico.calculateFrequency(movies.map(_.original_language))
      val topLanguages = Estadistico.topN(languageFreq, 5)

      // Imprimir resultados
      IO.println("=" * 70) >>
        IO.println("    ANÁLISIS ESTADÍSTICO DE PELÍCULAS") >>
        IO.println("=" * 70) >>
        IO.println(s"\nTotal de películas analizadas: $n") >>
      // INGRESOS (Revenue)
        IO.println("\n[1] ESTADÍSTICAS DE INGRESOS (Revenue)") >>
        IO.println("-" * 70) >>
        IO.println(f" Películas con revenue = 0: $revenueZeros%,d (${revenueZeros*100.0/n}%.1f%%)") >>
        IO.println(f"  Películas con revenue > 0: ${revenueNonZero.length}%,d (${revenueNonZero.length*100.0/n}%.1f%%)") >>
        IO.println("\n TODOS LOS DATOS (incluyendo ceros):") >>
        printNumericStats(revenueStats) >>
        (if (revenueNonZero.nonEmpty) {
          IO.println(f"\nSOLO PELÍCULAS CON INGRESOS REPORTADOS (${revenueNonZero.length}%,d películas):") >>
            printNumericStats(revenueStatsNonZero)
        } else IO.unit) >>
      // PRESUPUESTO (Budget)
        IO.println("\n[2] ESTADÍSTICAS DE PRESUPUESTO (Budget)") >>
        IO.println("-" * 70) >>
        IO.println(f"Películas con budget = 0: $budgetZeros%,d (${budgetZeros*100.0/n}%.1f%%)") >>
        IO.println(f"Películas con budget > 0: ${budgetNonZero.length}%,d (${budgetNonZero.length*100.0/n}%.1f%%)") >>
        IO.println("\n TODOS LOS DATOS (incluyendo ceros):") >>
        printNumericStats(budgetStats) >>
        (if (budgetNonZero.nonEmpty) {
          IO.println(f"\n SOLO PELÍCULAS CON PRESUPUESTO REPORTADO (${budgetNonZero.length}%,d películas):") >>
            printNumericStats(budgetStatsNonZero)
        } else IO.unit) >>
      // DURACIÓN (Runtime)
      IO.println("\n[3] ESTADÍSTICAS DE DURACIÓN (Runtime)") >>
        IO.println("-" * 70) >>
        printNumericStats(runtimeStats) >>
      // CALIFICACIÓN (Vote Average)
      IO.println("\n[4] ESTADÍSTICAS DE CALIFICACIÓN (Vote Average)") >>
        IO.println("-" * 70) >>
        printNumericStats(voteStats) >>
      // IDIOMAS
      IO.println("\n[5] TOP 5 IDIOMAS MÁS FRECUENTES") >>
        IO.println("-" * 70) >>
        topLanguages.traverse { case (lang, count) =>
          val percentage = count.toDouble / n * 100
          IO.println(f"  $lang%-10s: $count%5d películas ($percentage%5.2f%%)")
        }.void >>
        IO.println("=" * 70)
    }
  }

  /**
   * Análisis bivariable: correlaciones entre variables
   */
  def analyzeBivariable(movies: List[Movie]): IO[Unit] = {
    if (movies.isEmpty) {
      IO.println("ERROR: No hay datos para analizar")
    } else {
      // Filtrar películas con datos completos
      val validMovies = movies.filter(m =>
        m.budget > 0 && m.revenue > 0 && m.vote_average > 0
      )

      val totalMovies: Int = movies.length
      val validCount:Int = validMovies.length
      val invalidCount: Int = totalMovies - validCount

      if (validMovies.length < 2) {
        IO.println("ERROR: No hay suficientes datos válidos para análisis bivariable")
      } else {
        val budgets = validMovies.map(_.budget)
        val revenues = validMovies.map(_.revenue)
        val votes = validMovies.map(_.vote_average)
        val popularity = validMovies.map(_.popularity)

        // Calcular correlaciones
        val corrBudgetRevenue = Estadistico.pearsonCorrelation(budgets, revenues)
        val corrBudgetVote = Estadistico.pearsonCorrelation(budgets, votes)
        val corrRevenueVote = Estadistico.pearsonCorrelation(revenues, votes)
        val corrVotePopularity = Estadistico.pearsonCorrelation(votes, popularity)

        IO.println("\n" + "=" * 70) >>
          IO.println("ANÁLISIS BIVARIABLE - CORRELACIONES") >>
          IO.println("=" * 70) >>
          IO.println(s"\nPelículas totales:     $totalMovies") >>
          IO.println(f"Películas analizadas:  $validCount%,d (${validCount*100.0/totalMovies}%.1f%%)") >>
          IO.println(f"Películas excluidas:   $invalidCount%,d (${invalidCount*100.0/totalMovies}%.1f%%)") >>
          IO.println(f"(excluidas por tener budget=0, revenue=0 o vote=0)") >>
          IO.println("\nCOEFICIENTES DE CORRELACIÓN DE PEARSON:") >>
          IO.println("-" * 70) >>
          IO.println(f"Budget vs Revenue:      $corrBudgetRevenue%7.4f  ${interpretCorrelation(corrBudgetRevenue)}") >>
          IO.println(f"Budget vs Vote Avg:     $corrBudgetVote%7.4f  ${interpretCorrelation(corrBudgetVote)}") >>
          IO.println(f"Revenue vs Vote Avg:    $corrRevenueVote%7.4f  ${interpretCorrelation(corrRevenueVote)}") >>
          IO.println(f"Vote Avg vs Popularity: $corrVotePopularity%7.4f  ${interpretCorrelation(corrVotePopularity)}") >>
          IO.println("\nINTERPRETACIÓN:") >>
          IO.println("* Correlación cercana a +1 indica relación positiva fuerte") >>
          IO.println("* Correlación cercana a -1 indica relación negativa fuerte") >>
          IO.println("* Correlación cercana a  0 indica poca o ninguna relación") >>
          IO.println("=" * 70)
      }
    }
  }


  /**
   * Helper: Interpreta el valor de correlación
   */
  def interpretCorrelation(r: Double): String = {
    val absR: Double = math.abs(r)
    val strength: String = if (absR >= 0.7) "Fuerte"
    else if (absR >= 0.4) "Moderada"
    else if (absR >= 0.2) "Débil"
    else "Muy débil/Ninguna"

    val direction = if (r > 0) "positiva" else if (r < 0) "negativa" else "sin dirección"
    s"($strength $direction)"
  }

  /**
   * Helper: Imprime estadísticas numéricas formateadas
   */
  def printNumericStats(stats: NumEstadisticas): IO[Unit] = {
    IO.println(f"  Conteo:                ${stats.count}%,d") >>
      IO.println(f"  Promedio (Media):      $$${stats.mean}%,.2f") >>
      IO.println(f"  Mediana:               $$${stats.median}%,.2f") >>
      IO.println(f"  Desviación Estándar:   $$${stats.stdDev}%,.2f") >>
      IO.println(f"  Mínimo:                $$${stats.min}%,.2f") >>
      IO.println(f"  Máximo:                $$${stats.max}%,.2f") >>
      IO.println(f"  Q1 (25%%):              $$${stats.q1}%,.2f") >>
      IO.println(f"  Q3 (75%%):              $$${stats.q3}%,.2f") >>
      IO.println(f"  IQR (Rango IntQ):      $$${stats.iqr}%,.2f")
  }
}
```

Explicacion

```scala
def analyzeMovieStats(movies: List[Movie]): IO[Unit] = {
    if (movies.isEmpty) {
      IO.println("ERROR: No hay datos para analizar")
    } else {
      val n = movies.length

      // Calcular estadísticas INCLUYENDO ceros
      val revenueStats = Estadistico.calculateStats(movies.map(_.revenue))
      val budgetStats = Estadistico.calculateStats(movies.map(_.budget))
      val runtimeStats = Estadistico.calculateStats(movies.map(_.runtime).filter(_ > 0))
      val voteStats = Estadistico.calculateStats(movies.map(_.vote_average).filter(_ > 0))

      //Calcular estadísticas SIN ceros
      val revenueNonZero = movies.map(_.revenue).filter(_ > 0)
      val budgetNonZero = movies.map(_.budget).filter(_ > 0)

      val revenueStatsNonZero = Estadistico.calculateStats(revenueNonZero)
      val budgetStatsNonZero = Estadistico.calculateStats(budgetNonZero)

      // Contar películas con valores en 0
      val revenueZeros = movies.count(_.revenue == 0.0)
      val budgetZeros = movies.count(_.budget == 0.0)

      // Análisis de idiomas
      val languageFreq = Estadistico.calculateFrequency(movies.map(_.original_language))
      val topLanguages = Estadistico.topN(languageFreq, 5)
```
Este bloque de código es el controlador central del análisis, su función principal es transformar la lista de objetos Movie en datos numéricos legibles, separando los datos "reales" de los valores faltantes (ceros).

``val n = movies.length``: Almacena el tamaño total del dataset en una variable inmutable.

Extracción y Cálculo General (Incluye Ceros)
En estas líneas, transformas la lista de objetos en listas de números simples para procesarlas.

``val revenueStats`` = Estadistico.calculateStats(movies.map(_.revenue)):

``.map(_.revenue)`` extrae solo los ingresos de cada película.

Envía esa lista a tu función de estadísticas para obtener media, desviación, etc.

``val budgetStats = ...``: Lo mismo que el anterior, pero con los presupuestos.

``val runtimeStats = ...filter(_ > 0)``: Extrae la duración, pero descarta las que marcan 0 minutos antes de calcular estadísticas, ya que una película no puede durar 0 minutos (se considera error de datos).

``val voteStats = ...filter(_ > 0)``: Extrae el promedio de votos, ignorando las películas que no han recibido votos (marcadas con 0).

Análisis de Datos "Limpios" (Sin Ceros)
``val revenueNonZero = movies.map(_.revenue).filter(_ > 0)``: Crea una lista que contiene únicamente películas con ingresos registrados.

``val revenueStatsNonZero = Estadistico.calculateStats(revenueNonZero)``: Calcula las estadísticas (como el ingreso promedio) basándose solo en películas que generaron dinero. Esto suele dar un promedio mucho más realista.

``val budgetStatsNonZero`` = ...: Lo mismo para el presupuesto, ignorando las películas con presupuesto desconocido (0).

Diagnóstico de Calidad de Datos
``val revenueZeros = movies.count(_.revenue == 0.0)``: Cuenta cuántas películas tienen un 0.0 en la columna de ingresos.

``val budgetZeros = movies.count(_.budget == 0.0)``: Cuenta cuántas tienen un 0.0 en la columna de presupuesto.

Análisis Categórico (Idiomas)
``val languageFreq = Estadistico.calculateFrequency(...)``:

Extrae todos los códigos de idioma (en, es, fr).

Usa la función que analizamos antes (la de identity y groupBy) para contar cuántas películas hay por idioma.

``val topLanguages = Estadistico.topN(languageFreq, 5)``: Toma ese mapa de frecuencias y extrae solo los 5 idiomas más comunes

```scala
def analyzeBivariable(movies: List[Movie]): IO[Unit] = {
    if (movies.isEmpty) {
      IO.println("ERROR: No hay datos para analizar")
    } else {
      // Filtrar películas con datos completos
      val validMovies: List[Movie] = movies.filter(m =>
        m.budget > 0 && m.revenue > 0 && m.vote_average > 0
      )

      val totalMovies : Int = movies.length
      val validCount: Int = validMovies.length
      val invalidCount: Int = totalMovies - validCount

      if (validMovies.length < 2) {
        IO.println("ERROR: No hay suficientes datos válidos para análisis bivariable")
      } else {
        val budgets: List[Double] = validMovies.map(_.budget)
        val revenues: List[Double] = validMovies.map(_.revenue)
        val votes: List[Double] = validMovies.map(_.vote_average)
        val popularity: List[Double] = validMovies.map(_.popularity)

        // Calcular correlaciones
        val corrBudgetRevenue: Double = Estadistico.pearsonCorrelation(budgets, revenues)
        val corrBudgetVote: Double = Estadistico.pearsonCorrelation(budgets, votes)
        val corrRevenueVote: Double = Estadistico.pearsonCorrelation(revenues, votes)
        val corrVotePopularity: Double = Estadistico.pearsonCorrelation(votes, popularity)
```

Utiliza la clase estadistico para hacer el calculo de las relaciones y correlaciones

### Limpieza

```scala
import models.Movie

object Limpieza {

  /**
   * Valida que un ID sea válido (no vacío y solo dígitos)
   */
  def isValidId(idStr: String): Boolean = {
    idStr.trim.nonEmpty && idStr.trim.forall(_.isDigit)
  }

  /**
   * Filtra películas válidas basándose en criterios personalizables
   */
  def filterValidMovies(movies: List[Movie],
                        minBudget: Double = 0.0,
                        minRevenue: Double = 0.0): List[Movie] = {
    movies.filter { m =>
      m.budget >= minBudget &&
        m.revenue >= minRevenue &&
        m.id > 0
    }
  }

  /**
   * Elimina duplicados basándose en el ID
   */
  def removeDuplicatesById(movies: List[Movie]): List[Movie] = {
    movies.groupBy(_.id).values.map(_.head).toList
  }

  /**
   * Normaliza texto: trim, lowercase, etc.
   */
  def normalizeText(text: String): String = {
    text.trim.toLowerCase.replaceAll("\\s+", " ")
  }

  /**
   * Limpia valores extremos (outliers) basándose en percentiles
   */
  def removeOutliers(values: List[Double], lowerPercentile: Double = 0.01, upperPercentile: Double = 0.99): List[Double] = {
    if (values.isEmpty) return List.empty

    val sorted: List[Double] = values.sorted
    val n: Int = sorted.length
    val lowerIndex: Int = (n * lowerPercentile).toInt
    val upperIndex: Int = (n * upperPercentile).toInt

    sorted.slice(lowerIndex, upperIndex)
  }
}
```

Tiene varias funciones como ejemplo *removeOutliers*, *normalizeText*, *filterValidMovies*,*removeDuplicatesById*,*isValidId* que sirven para calcular estaditicas y/o presentar datos




## Parsear_JSON.scala

### Ubicación
`utilities/Parsear_JSON.scala`

### Propósito

Este objeto se encarga de limpiar y convertir strings con formato JSON no estándar (estilo Python) en JSON válido, permitiendo su posterior parseo con **Circe**.

---

### Importaciones

```scala
import io.circe.parser.decode
import io.circe.Decoder
import io.circe.generic.auto._
import cats.syntax.either._
```

- `decode`: Convierte un String JSON en un objeto Scala.
- `Decoder`: Define cómo transformar JSON en tipos Scala.
- `generic.auto`: Genera decoders automáticamente desde case class.
- `cats.syntax.either`: Permite trabajar de forma funcional con `Either`.

---

### Limpieza de JSON

```scala
def cleanJsonString(raw: String): String = {
  if (raw == null) return "[]"
  val trimmed = raw.trim
  if (trimmed.isEmpty || trimmed == "null" || trimmed.equalsIgnoreCase("nan")) return "[]"

  trimmed
    .replace("'", """)
    .replace("None", "null")
    .replace("True", "true")
    .replace("False", "false")
    .replace("nan", "null")
    .replace(""{", "{").replace("}"", "}")
}
```

Esta función:
- Normaliza JSON mal formateado.
- Convierte valores de Python a JSON estándar.
- Evita errores devolviendo listas vacías cuando el contenido es inválido.

---

### Parseo de listas JSON

```scala
def parseJsonField[T](rawJson: String)(implicit decoder: Decoder[List[T]]): List[T] = {
  val jsonLimpio = cleanJsonString(rawJson)
  if (jsonLimpio == "[]") return List.empty

  decode[List[T]](jsonLimpio).getOrElse(List.empty)
}
```

- Convierte un campo JSON que representa una lista.
- Usa `Decoder[List[T]]`.
- Si falla, devuelve una lista vacía sin detener el programa.

---

### Parseo de objetos JSON únicos

```scala
def parseJsonFieldSingle[T](rawJson: String)(implicit decoder: Decoder[T]): Option[T] = {
  val jsonLimpio = cleanJsonString(rawJson)
  if (jsonLimpio == "[]" || jsonLimpio == "{}") return None

  decode[T](jsonLimpio).toOption
}
```

- Usado cuando el JSON representa un único objeto.
- Devuelve `Option[T]` para manejar valores ausentes.

---

## Archivo: PoblarBaseDatos.scala

### Ubicación
`utilities/PoblarBaseDatos.scala`

### Propósito

Contiene la lógica de inserción masiva de datos en la base de datos usando **Doobie**, optimizada mediante batch inserts.

---

### Conversión segura de tipos

```scala
def safeInt(s: String): Int = s.trim.toDoubleOption.map(_.toInt).getOrElse(0)
def safeDouble(s: String): Double = s.trim.toDoubleOption.getOrElse(0.0)
```

Estas funciones:
- Evitan excepciones por datos corruptos.
- Garantizan valores por defecto en caso de error.

---

### Case class de parámetros

```scala
private case class PeliculaParam(
  idPelicula: Int,
  imdb_id: String,
  title: String,
  original_title: String,
  overview: String,
  tagline: String,
  adult: Int,
  video: Int,
  status: String,
  release_date: String,
  budget: Double,
  revenue: Double,
  runtime: Int,
  popularity: Double,
  vote_average: Double,
  vote_count: Int,
  homepage: String,
  poster_path: String,
  original_language: String
)
```

Esta clase:
- Representa exactamente los campos de la tabla `Peliculas`.
- Permite preparar datos limpios antes del insert.

---

### Construcción segura de datos

```scala
private def buildPeliculaParam(row: Map[String, String]): Option[PeliculaParam] = {
  val pId = safeInt(row.getOrElse("id", "0"))
  if (pId == 0) None
  else {
    Some(
      PeliculaParam(
        pId,
        row.getOrElse("imdb_id", ""),
        row.getOrElse("title", ""),
        row.getOrElse("original_title", ""),
        row.getOrElse("overview", ""),
        row.getOrElse("tagline", ""),
        if (row.getOrElse("adult", "false").contains("true")) 1 else 0,
        if (row.getOrElse("video", "false").contains("true")) 1 else 0,
        row.getOrElse("status", "Released"),
        row.getOrElse("release_date", "1900-01-01"),
        safeDouble(row.getOrElse("budget", "0")),
        safeDouble(row.getOrElse("revenue", "0")),
        safeInt(row.getOrElse("runtime", "0")),
        safeDouble(row.getOrElse("popularity", "0")),
        safeDouble(row.getOrElse("vote_average", "0")),
        safeInt(row.getOrElse("vote_count", "0")),
        row.getOrElse("homepage", ""),
        row.getOrElse("poster_path", ""),
        row.getOrElse("original_language", "un")
      )
    )
  }
}
```

- Valida el ID de la película.
- Aplica conversiones seguras.
- Devuelve `None` si el registro no es válido.

---

### Inserción por lotes

```scala
def populateBatch(rows: List[Map[String, String]]): ConnectionIO[Unit] = {
  val peliculas = rows.flatMap(buildPeliculaParam)
  ...
}
```

- Inserta datos en múltiples tablas.
- Usa `INSERT IGNORE` para evitar duplicados.
- Ejecuta todo dentro de una transacción.

---

## Archivo: Main.scala

### Ubicación
`Main.scala`

### Propósito

Es el punto de entrada de la aplicación. Orquesta la lectura del CSV, la carga a base de datos y las fases de análisis.

---

### Configuración del pipeline

```scala
val BATCH_SIZE: Int = 1000
val SKIP_ANALYSIS: Boolean = false
val DISABLE_FK_CHECKS: Boolean = true
```

- Permite controlar el rendimiento y el comportamiento del sistema.

---

### Pipeline optimizado

```scala
def pipelineOptimizado(
  transactor: Transactor[IO],
  rows: List[Map[String, String]]
): IO[Unit] = {
  ...
}
```

- Divide los datos en lotes.
- Desactiva claves foráneas para mejorar rendimiento.
- Ejecuta inserciones por batch.

---

### Procesamiento con FS2

```scala
def procesarLotes(
  transactor: Transactor[IO],
  batches: List[List[Map[String, String]]]
): IO[Unit] = {
  fs2.Stream
    .emits(batches.zipWithIndex)
    .evalMap { case (batch, idx) => ... }
    .compile
    .drain
}
```

- Procesa cada lote de forma controlada.
- Maneja errores sin detener toda la ejecución.

---

### Método principal

```scala
def run: IO[Unit] = {
  ConexionDB.xa.use { transactor =>
    ...
  }
}
```

- Inicializa la conexión.
- Ejecuta carga de datos.
- Lanza análisis exploratorio.

