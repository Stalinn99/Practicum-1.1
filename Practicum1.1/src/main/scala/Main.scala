import cats.effect.{IO, IOApp}
import cats.implicits.*
import fs2.io.file.{Files, Path}
import models.*
import data.*
import utilities.*
import io.circe.generic.auto.*
import fs2.data.csv.lenient.attemptDecodeUsingHeaders
import doobie.implicits.toConnectionIOOps

object Main extends IOApp.Simple {

  val filePath: Path = Path("src/main/resources/data/pi_movies_complete (3).csv")

  def pipeline(transactor: doobie.Transactor[IO]):IO[Unit] =
    Files[IO].readAll(filePath)
      .through(fs2.text.utf8.decode)
      .through(attemptDecodeUsingHeaders[Map[String, String]](separator = ';'))
      .zipWithIndex
      .evalMap {
        case (Right(row), index) =>
          PoblarBaseDatos.populateAll(row)
            .transact(transactor)
            .handleErrorWith { e =>
              PoblarBaseDatos.logError(index, e)
            } *>
            PoblarBaseDatos.logProgress(index, interval = 1000)
        case (Left(error), index) =>
          IO.println(s"Fila $index ignorada por formato inválido: ${error.getMessage}")
      }
      .compile.drain

  def run: IO[Unit] = {
    val transactor = ConexionDB.xaSimple
    for {
      _ <- printHeader("ANÁLISIS EXPLORATORIO DE DATOS - PELÍCULAS (EDA)")

      // FASE 1: Carga y Limpieza
      _ <- IO.println("\n>>> FASE 1: CARGA Y LIMPIEZA DE DATOS")
      results <- LecturaCSV.readMoviesFromCsv(filePath)
      movies = results.collect { case Right(m) => m }
      errors = results.collect { case Left(_) => 1 }.length

      _ <- IO.println(s"Filas procesadas exitosamente: ${movies.length}")
      _ <- IO.println(s"Filas con errores de lectura: $errors")

      moviesClean = Limpieza.removeDuplicatesById(movies)
      _ <- IO.println(s"Duplicados eliminados: ${movies.length - moviesClean.length}")
      _ <- IO.println(s"Películas antes de limpiar: ${movies.length}")
      _ <- IO.println(s"Películas sin duplicados: ${moviesClean.length}")

      _ <- IO.println(s"\n>>> DEBUG VOTOS:")
      _ <- IO.println(s"    Con duplicados - Películas con vote_count > 0: ${movies.count(_.vote_count > 0.0)}")
      _ <- IO.println(s"    Con duplicados - Total de votos: ${movies.filter(_.vote_count > 0.0).map(_.vote_count).sum.toLong}")
      _ <- IO.println(s"    Sin duplicados - Películas con vote_count > 0: ${moviesClean.count(_.vote_count > 0.0)}")
      _ <- IO.println(s"    Sin duplicados - Total de votos: ${moviesClean.filter(_.vote_count > 0.0).map(_.vote_count).sum.toLong}")

      // FASE 2 y 3: Análisis Univariable y Bivariable
      _ <- IO.println("\n>>> FASE 2: ANÁLISIS UNIVARIABLE")
      _ <- AnalisisMovie.analyzeMovieStats(moviesClean)

      _ <- IO.println("\n>>> FASE 3: ANÁLISIS BIVARIABLE")
      _ <- AnalisisMovie.analyzeBivariable(moviesClean)

      // FASE 4 - 12: Análisis de Entidades (usando función genérica)
      _ <- mostrarAnalisisJson("GÉNEROS", LecturaJSON.analyzeGenres(filePath))
      _ <- mostrarAnalisisJson("ROLES EN PRODUCCIÓN", LecturaJSON.analyzeCrewByJob(filePath))
      _ <- mostrarAnalisisJson("PALABRAS CLAVE", LecturaJSON.analisisKeyWords(filePath))
      _ <- mostrarAnalisisJson("IDIOMAS HABLADOS", LecturaJSON.analisisSpokenLenguaje(filePath))
      _ <- mostrarAnalisisJson("COLECCIONES", LecturaJSON.analisisColecciones(filePath))
      _ <- mostrarAnalisisJson("COMPAÑÍAS PRODUCTORAS", LecturaJSON.analisisCompanias(filePath))
      _ <- mostrarAnalisisJson("PAÍSES PRODUCTORES", LecturaJSON.analisisPaises(filePath))

      // FASE 13: Validación de IDs
      _ <- IO.println("\n>>> FASE 13: VALIDACIÓN DE IDs")
      count <- LecturaCSV.countValidRows(filePath, "id", Limpieza.isValidId)
      _ <- printSection(s"Total de películas con ID válido: $count")

      // FASE 14: Cast
      _ <- IO.println("\n>>> FASE 14: ANÁLISIS DE CAST (ACTORES)")
      _ <- analizarCast(moviesClean)

      // FASE 15: Ratings
      _ <- IO.println("\n>>> FASE 15: ANÁLISIS DE RATINGS")
      _ <- mostrarAnalisisRatings()

      // FASE 16: Fechas
      _ <- IO.println("\n>>> FASE 16: ANÁLISIS DE FECHAS DE ESTRENO")
      _ <- mostrarAnalisisFechas()

      // FASE 17: Población BD
      _ <- IO.println("\n>>> FASE 17: POBLACIÓN DE BASE DE DATOS")
      _ <- pipeline(transactor)

      _ <- printHeader("PROCESO TERMINADO EXITOSAMENTE")
    } yield ()
  }

  // ============= FUNCIONES AUXILIARES PARA FORMATEO =============

  def printHeader(text: String): IO[Unit] =
    IO.println("\n" + "=" * 70) >>
      IO.println(text) >>
      IO.println("=" * 70)

  def printSection(text: String): IO[Unit] =
    IO.println("=" * 70) >>
      IO.println(text) >>
      IO.println("=" * 70)

  /**
   * Función genérica que maneja todos los análisis de frecuencias
   * de campos JSON
   */
  def mostrarAnalisisJson(
                           titulo: String,
                           ioData: IO[Map[String, Int]],
                           topN: Int = 15
                         ): IO[Unit] = {
    for {
      datos <- ioData.handleErrorWith { error =>
        IO.println(s"Error al leer $titulo: ${error.getMessage}") *>
          IO.pure(Map.empty[String, Int])
      }
      _ <- if (datos.isEmpty) {
        IO.println(s"\n>>> FASE: $titulo")
        IO.println("No se encontraron datos")
      } else {
        val total = datos.values.sum
        val topItems = Estadistico.topN(datos, topN)

        IO.println(s"\n>>> FASE: $titulo") >>
          printSection(s"TOP $topN - $titulo") >>
          IO.println(f"Total único: ${datos.size}%,d") >>
          IO.println(f"Total de relaciones: $total%,d\n") >>
          topItems.zipWithIndex.traverse { case ((item, count), idx) =>
            val porcentaje = if (total > 0) count.toDouble / total * 100 else 0.0
            IO.println(f"  ${idx + 1}%2d. $item%-40s: $count%6d ($porcentaje%5.2f%%)")
          }.void >>
          printSection("")
      }
    } yield ()
  }

  /**
   * Análisis de Cast con estadísticas adicionales
   */
  def analizarCast(movies: List[Movie]): IO[Unit] = {
    try {
      val actores: List[Cast] = movies
        .flatMap { movie =>
          Parsear_JSON.parseJsonField[Cast](movie.cast)
        }

      val actoresPorNombre: Map[String, Int] = actores
        .map(_.name)
        .groupBy(identity)
        .view.mapValues(_.length)
        .toMap

      val topActores = Estadistico.topN(actoresPorNombre,15)
      val totalActores = actoresPorNombre.size
      val peliculasConCast = movies.count(c => c.cast.trim != "[]" && c.cast.trim.nonEmpty)
      val promedioActoresPorPelicula = if (peliculasConCast > 0) actores.length.toDouble / peliculasConCast else 0.0

      printSection("ESTADÍSTICAS DE CAST (ACTORES)") >>
        IO.println(f"Total de películas con cast:              $peliculasConCast%,d") >>
        IO.println(f"Total de actores únicos:                  $totalActores%,d") >>
        IO.println(f"Total de apariciones de actores:          ${actores.length}%,d") >>
        IO.println(f"Promedio de actores por película:         $promedioActoresPorPelicula%.2f") >>
        IO.println("\nTOP 20 ACTORES CON MÁS PELÍCULAS:") >>
        IO.println("-" * 70) >>
        topActores.zipWithIndex.traverse { case ((actor, count), idx) =>
          IO.println(f"  ${idx + 1}%2d. $actor%-30s: $count%3d películas")
        }.void >>
        printSection("")
    } catch {
      case e: Exception =>
        IO.println(s"Error en análisis de cast: ${e.getMessage}") >>
          printSection("")
    }
  }

  /**
   * Análisis de Ratings
   */
  def mostrarAnalisisRatings(): IO[Unit] = {
    for {
      stats <- LecturaJSON.analisisRatings(filePath)
      _ <- printSection("ANÁLISIS COMPLETO DE RATINGS") >>
        IO.println(f"Total de ratings:                     ${stats.totalRatings}%,d") >>
        IO.println(f"Total de userId (con repeticiones):   ${stats.totalUserIds}%,d") >>
        IO.println(f"Usuarios únicos:                      ${stats.usuariosUnicos}%,d") >>
        IO.println(f"Películas con ratings:                ${stats.peliculasConRatings}%,d") >>
        IO.println(f"Promedio ratings por película:        ${stats.promedioRatingsPorPelicula}%.2f") >>
        IO.println(f"Promedio ratings por usuario único:   ${if(stats.usuariosUnicos > 0) stats.totalRatings.toDouble / stats.usuariosUnicos else 0.0}%.2f") >>
        printSection("")
    } yield ()
  }

  /**
   * Análisis de Fechas
   */
  def mostrarAnalisisFechas(): IO[Unit] = {
    for {
      fechaStats <- LecturaJSON.analizarFechasEstreno(filePath)
      tasaFechas = if (fechaStats.totalFilas > 0) fechaStats.fechasValidas.toDouble / fechaStats.totalFilas * 100
        else 0.0
      _ <- printSection("ANÁLISIS DE FECHAS DE ESTRENO") >>
        IO.println(f"Total de filas:                            ${fechaStats.totalFilas}%,d") >>
        IO.println(f"Fechas válidas parseadas:                  ${fechaStats.fechasValidas}%,d") >>
        IO.println(f"Fechas inválidas o vacías:                 ${fechaStats.fechasInvalidas}%,d") >>
        IO.println(f"Tasa de éxito:                             $tasaFechas%.2f%%") >>
        IO.println("\nEjemplos de fechas parseadas (formato yyyy/MM/dd):") >>
        fechaStats.ejemplosFechasParseadas.take(5).traverse_ { case (original, parseada) =>
          IO.println(f"  $original%-15s -> $parseada")
        } >>
        printSection("")
    } yield ()
  }
}