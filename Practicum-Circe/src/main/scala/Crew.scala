import _root_.io.circe.generic.auto.*
import _root_.io.circe.parser.decode
import cats.effect.*
import fs2.*
import fs2.data.csv.*
import fs2.io.file.*

// Definimos el Case Class con todos los campos del JSON
case class Crew(
                  credit_id: String,
                  department: String,
                  gender: Int,
                  id: Int,
                  job: String,
                  name: String,
                  profile_path: Option[String], // Option porque hay valores "None" (null)
                  overview : String
               )

object Crew extends IOApp.Simple {
  val rutaArchivo: Path = Path("src/main/resources/data/pi_movies_complete (3) - copia.csv")
  
  def limpiarYParsear(rawJson: String): List[Crew] = {
    val limpio = rawJson.trim
    if (limpio.isEmpty || limpio == "[]" || limpio == "nan") return List.empty

    val jsonLimpio = limpio
      .replace("'", "\"")
      .replace("None", "null")
    // Intentamos decodificar como Lista de Crew
    decode[List[Crew]](jsonLimpio) match {
      case Right(lista) => lista
      case Left(error)  => List.empty
    }
  }

  val run: IO[Unit] = {
    Files[IO]
      .readAll(rutaArchivo)
      .through(fs2.text.utf8.decode)
      // Usamos el separador ';' que mencionaste
      .through(decodeUsingHeaders[CsvRow[String]](separator = ';'))
      .attempt
      .map {
        case Right(row) =>
          val crewRaw: String = row("crew").getOrElse("[]")
          val listaData: List[Crew] = limpiarYParsear(crewRaw)

          // Creamos un mapa de conteo por 'job' para esta fila
          listaData.foldLeft(Map.empty[String, Int]) { (acc, persona) =>
            acc + (persona.job -> (acc.getOrElse(persona.job, 0) + 1))
          }
        case Left(_) => Map.empty[String, Int]
      }
      // Combinamos los conteos de todas las filas en un solo mapa final
      .fold(Map.empty[String, Int]) { (mapaAcumulado, mapaFila) =>
        mapaFila.foldLeft(mapaAcumulado) { case (acc, (job, cantidad)) =>
          acc + (job -> (acc.getOrElse(job, 0) + cantidad))
        }
      }
      .evalMap { mapaFinal =>
        if (mapaFinal.isEmpty) {
          IO.println("No se encontraron datos o el archivo está vacío.")
        } else {
          IO.println("=== CONTEO TOTAL POR TRABAJO (JOB) ===")
          // Ordenamos por cantidad de mayor a menor
          val salida = mapaFinal.toList.sortBy(-_._2).map { case (job, cant) =>
            s"$job: $cant"
          }.mkString("\n")
          IO.println(salida)
        }
      }
      .compile
      .drain
  }
}