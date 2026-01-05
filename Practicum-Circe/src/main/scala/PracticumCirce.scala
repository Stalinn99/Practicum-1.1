import cats.effect.*
import fs2.*
import fs2.io.file.*
import fs2.data.csv.*
import _root_.io.circe.generic.auto.*
import _root_.io.circe.parser.decode
case class Crew(
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

  def limpiarYParsear(rawJson: String): List[Crew] = {
    if (rawJson.trim.isEmpty || rawJson == "[]") return List.empty
    val jsonLimpio = rawJson.replace("'", "\"").replace("None", "null")
    decode[List[Crew]](jsonLimpio).getOrElse(List.empty)
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
          val listaData: List[Crew] = limpiarYParsear(crewRaw)
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