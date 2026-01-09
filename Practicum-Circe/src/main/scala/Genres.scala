import _root_.io.circe.generic.auto.*
import _root_.io.circe.parser.decode
import cats.effect.*
import cats.syntax.all.*
import fs2.*
import fs2.data.csv.*
import fs2.io.file.*
case class Genres(
                   id: Int,
                   name: String
                 )
object GenreCsvProcessor extends IOApp.Simple {
  val rutaArchivo: Path = Path("src/main/resources/data/pi_movies_complete (3) - copia.csv")
  // Función de limpieza genérica
  def limpiarYParsear(rawJson: String): List[Genres] = {
    if (rawJson.trim.isEmpty || rawJson == "[]") return List.empty
    // Reemplazamos comillas simples por dobles y None por null para que sea JSON válido
    val jsonLimpio = rawJson.replace("'", "\"").replace("None", "null")

    decode[List[Genres]](jsonLimpio).getOrElse(List.empty)
  }

  val run: IO[Unit] = {
    Files[IO]
      .readAll(rutaArchivo)
      .through(fs2.text.utf8.decode)
      .through(decodeUsingHeaders[CsvRow[String]](separator = ';'))
      .attempt
      .map {
        case Right(row) =>
          val genresRow : String = row("genres").getOrElse("[]")
          val listageneros : List[Genres] = limpiarYParsear(genresRow)
          listageneros.foldLeft(Map.empty[String,Int]){(acc, genero) =>
            val conteoActual : Int = acc.getOrElse(genero.name, 0)
            acc + (genero.name -> (conteoActual + 1))
          }
        case Left (_) =>
          Map.empty[String,Int]
      }
      .fold(Map.empty[String,Int]){(mapaAcummulado, mapaFila)=>
        mapaFila.foldLeft(mapaAcummulado){case (acc,(nombre, cantidad)) =>
          val totalSuma: Int = acc.getOrElse(nombre,0) + cantidad
          acc + (nombre -> totalSuma)
        }
      }
      .evalMap { mapaFinal =>
        IO.println("--- CONTEO POR GÉNERO ---") *>
          mapaFinal.toList.sortBy(-_._2).map { case (gen, cant) =>
            IO.println(s"$gen: $cant")
          }.sequence.void
      }
      .compile
      .drain
  }
}