# PROYECTO INTEGRADOR PRACTICCUM1.1

## DOCUMENTACION:

### Package data:

LecturaCSV

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
## Contenido de la exportacion (CSVDecoder):<br>
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


