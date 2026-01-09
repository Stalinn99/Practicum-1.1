import cats.effect.{IO, IOApp}
import fs2.io.file.{Files, Path}
import fs2.data.csv._
import fs2.{Stream, text}

object CountMoviesById extends IOApp.Simple {

  val rutaArchivo: Path = Path("src/main/resources/data/pi_movies_complete (3) - copia.csv")

  val run: IO[Unit] = {
    IO.println("CONTEO ESTRICTO POR ID") >>
      Files[IO]
        .readAll(rutaArchivo)
        .through(text.utf8.decode)
        .through(decodeUsingHeaders[CsvRow[String]](separator = ';'))
        .map { row =>
          row("id")
        }
        .collect {
          // Filtramos: solo si existe el campo, no está vacío y todos sus caracteres son dígitos
          case Some(idStr) if idStr.trim.nonEmpty && idStr.trim.forall(_.isDigit) =>
            idStr
        }
        .compile
        .count // Esta función de FS2 cuenta cuántos elementos pasaron el filtro
        .flatMap { total =>
          IO.println("=" * 40) >>
            IO.println(s"TOTAL DE PELÍCULAS VÁLIDAS (Por ID): $total") >>
            IO.println("=" * 40)
        }
  }
}