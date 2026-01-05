# Practicum-1.1
# CIRCE

## ¿Qué es circe?<br>

Circe es una biblioteca de Scala que simplifica el trabajo con JSON <br>
permitiéndonos decodificar fácilmente una cadena JSON en un objeto <br>
de Scala o convertir un objeto de Scala a JSON . La biblioteca genera <br>
automáticamente los codificadores y decodificadores de objetos<br>
 reduciendo así las líneas de código necesarias para trabajar con JSON en Scala

---

## CODIGO DE DEPENDENCIA PARA USAR CIRCE

```scala
val circeVersion = "0.14.9"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
```

## VERIFICACION DE FUNCIONALIDAD

Para verificar que la libreria circe se ha instalado correctamente<br>
vamos a validar una estructura JSON dentro de Scala, y la siguiente cadena<br>de estructura JSON va a ser la siguiente:

```javascript
{
    "textField": "textContent",
    "numericField": 123,
    "booleanField": true,
    "nestedObject": {
        "arrayField": [1, 2, 3]
    }
}
```

### Estructura JSON sacado de: [Analizando JSON con Circe](https://www.baeldung.com/scala/circe-json)

## Codigo en Scala

```scala
import io.circe._, io.circe.parser._

val jsonString:String =
  """
    |{
    | "textField": "textContent",
    | "numericField": 123,
    | "booleanField": true,
    | "nestedObject": {
    | "arrayField": [1, 2, 3]
    | }
    |}
    |""".stripMargin

val parseResult: Either[ParsingFailure, Json] = parse(jsonString)
```

- Importamos la libreria Circe <br>

```scala
import io.circe._, io.circe.parser._
```

Se importa las clases bases y `io.circe.parser._` importa en especifico<br> el metodo de `parse`que se encarga de pasar una cadena de texto a un objeto JSON

```scala
val jsonString:String =
  """
    |{
    | "textField": "textContent",
    | "numericField": 123,
    | "booleanField": true,
    | "nestedObject": {
    | "arrayField": [1, 2, 3]
    | }
    |}
    |""".stripMargin
```

Las triples commilas permiten esccribir un texto en varias lineas sin usar caracteres de escape

El metodo `.stripMargin` se encarga de eliminar los espacios en blanco antes del caracter `|` para que al final el String está limpio

```scala
val parseResult: Either[ParsingFailure, Json] = parse(jsonString)
```

Aqui llamamos a la funcion `parse`<br>
La variable esta definia dentro de un tipo de dato `Either[A,B]`<br>
Usamos Either para controlar los psoibles errores que puede existir <br>
comunmente si algo falla Scala devuelve el caso de la izquierda <br>
que sería `ParsingFailure` caso contrario que el JSON<br>
no tuvo erroes nos devuelve un `JSON`

***
## SOLUCION A LA COLUMNA CREW

```scala
import cats.effect.*
import fs2.*
import fs2.io.file.*
import fs2.data.csv.*
import _root_.io.circe.generic.auto.*
import _root_.io.circe.parser.decode
case class CrewMember(
                       credit_id: String,
                       department: String,
                       gender: Int,
                       id: Int,
                       job: String,
                       name: String,
                       profile_path: Option[String]
                     )
object CsvJsonProcessor extends IOApp.Simple {
  val rutaArchivo: Path = Path("src/main/resources/data/pi_movies_complete (3) - copia.csv")

  def limpiarYParsear(rawJson: String): List[CrewMember] = {
    if (rawJson.trim.isEmpty || rawJson == "[]") return List.empty
    val jsonLimpio = rawJson.replace("'", "\"").replace("None", "null")
    decode[List[CrewMember]](jsonLimpio).getOrElse(List.empty)
  }
  val run: IO[Unit] = {
    Files[IO]
      .readAll(rutaArchivo)
      .through(fs2.text.utf8.decode)
      .through(decodeUsingHeaders[CsvRow[String]](separator = ';'))
      .attempt
      .map {
        case Right(row) =>
          val crewRaw: String = row("crew").getOrElse("[]")
          val listaData: List[CrewMember] = limpiarYParsear(crewRaw)
          val listaCompleta: Long = listaData.size.toLong
          val directingTotal: Long = listaData.count(_.department == "Directing").toLong
          // Entregamos ambos valores para que el fold los sume
          (listaCompleta, directingTotal)
        case Left(_) => (0L, 0L)
      }
      // fold recibe el valor inicial (0, 0) y luego suma cada par
      .fold((0L, 0L)) { case ((accTotal, accDirecting), (filaTotal, filaDirecting)) =>
        (accTotal + filaTotal, accDirecting + filaDirecting)
      }
      .evalMap { case (totalFinal, totalDirectores) =>
        IO.println(s"EL TOTAL FINAL ES: $totalFinal IDs") *>
          IO.println(s"EL TOTAL DE DIRECTING ES: $totalDirectores")
      }
      .compile
      .drain
  }
}
```

### Manejo de la Columna "Crew"
La columna `crew` del dataset presentaba un formato no estándar (comillas simples y valores `None` al estilo Python)<br>
Para solucionarlo, se implementó una función de limpieza:
* Reemplazo de `'` por `"` para cumplir el estándar JSON.
* Reemplazo de `None` por `null` para compatibilidad con Circe.
* Manejo de valores vacíos y errores de parseo mediante `getOrElse(List.empty)`.

### Procesamiento de Datos (FS2)
Para el manejo del archivo `pi_movies_complete`, se utilizó la librería **FS2 Data CSV**, permitiendo procesar miles de registros de forma reactiva (en stream) sin saturar la memoria RAM del equipo. 

Se implementó una búsqueda múltiple mediante el método `.fold`, obteniendo simultáneamente:
* El conteo total de IDs de personal técnico.
* El conteo específico del departamento de **Directing**.

### Resultados Obtenidos
Tras ejecutar el procesador sobre el dataset completo, se obtuvieron los siguientes datos finales:
* **Total de miembros del equipo técnico (IDs):** 33,343
* **Total de miembros en el departamento Directing:** 4,127
