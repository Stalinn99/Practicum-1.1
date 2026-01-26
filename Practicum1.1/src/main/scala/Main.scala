import cats.effect.{IO, IOApp}
import cats.implicits.*
import fs2.io.file.Path
import models.*
import data.*
import utilities.*
import io.circe.generic.auto.*
import doobie.implicits.toConnectionIOOps
import doobie.implicits.toSqlInterpolator
import doobie.*

object Main extends IOApp.Simple {

  val filePath: Path = Path("src/main/resources/data/pi_movies_complete (3).csv")

  // ============= CONFIGURACIÓN =============
  val BATCH_SIZE: Int = 1000
  val SKIP_ANALYSIS: Boolean = false
  val DISABLE_FK_CHECKS: Boolean = true
  val PRINT_BATCH_TIME: Boolean = true

  // ============= PIPELINE OPTIMIZADO =============

  def pipelineOptimizado(
      transactor: doobie.Transactor[IO],
      rows: List[Map[String, String]]
  ): IO[Unit] = {
    for {
      _ <- if (DISABLE_FK_CHECKS)
        sql"SET FOREIGN_KEY_CHECKS=0".update.run.transact(transactor)
      else IO.unit

      batches = rows.grouped(BATCH_SIZE).toList
      _ <- IO.println(s"📦 Total de lotes: ${batches.length}")
      _ <- procesarLotes(transactor, batches)

      _ <- if (DISABLE_FK_CHECKS)
        sql"SET FOREIGN_KEY_CHECKS=1".update.run.transact(transactor)
      else IO.unit

      _ <- IO.println(" Población de BD completada")
    } yield ()
  }

  def procesarLotes(
      transactor: doobie.Transactor[IO],
      batches: List[List[Map[String, String]]]
  ): IO[Unit] = {
    fs2.Stream
      .emits(batches.zipWithIndex)
      .covary[IO]
      .evalMap { case (batch, idx) =>
        for {
          t0 <- IO.delay(System.nanoTime())
          res <- PoblarBaseDatos.populateBatch(batch).transact(transactor).attempt
          t1 <- IO.delay(System.nanoTime())

          _ <- res match {
            case Left(e) =>
              IO.println(s" ERROR Batch $idx: ${e.getMessage}")
            case Right(_) =>
              val porcentaje = (idx + 1) * 100 / batches.length
              if (PRINT_BATCH_TIME) {
                IO.println(
                  s"✓ Batch ${idx + 1}/${batches.length} | " +
                    s"${batch.size} filas"
                )
              } else IO.unit
          }
        } yield ()
      }
      .compile
      .drain
  }

  // ============= ANÁLISIS =============

  def analisisfase2y3(moviesClean: List[Movie]): IO[Unit] = {
    IO.println("\n>>> FASE 2: ANÁLISIS UNIVARIABLE") *>
      AnalisisMovie.analyzeMovieStats(moviesClean) *>
      IO.println("\n>>> FASE 3: ANÁLISIS BIVARIABLE") *>
      AnalisisMovie.analyzeBivariable(moviesClean)
  }

  def analisisfases4a12(
      rows: List[Map[String, String]]
  ): IO[Unit] = {
    val analisisGeneros = IO {
      rows.flatMap(r => Parsear_JSON.parseJsonField[Genres](r.getOrElse("genres", "[]")))
        .foldLeft(Map.empty[String, Int]) { (acc, g) =>
          acc.updated(g.name, acc.getOrElse(g.name, 0) + 1)
        }
    }

    val analisisRoles = IO {
      rows.flatMap(r => Parsear_JSON.parseJsonField[Crew](r.getOrElse("crew", "[]")))
        .foldLeft(Map.empty[String, Int]) { (acc, c) =>
          acc.updated(c.job, acc.getOrElse(c.job, 0) + 1)
        }
    }

    val analisisKeywords = IO {
      rows.flatMap(r => Parsear_JSON.parseJsonField[Keywords](r.getOrElse("keywords", "[]")))
        .foldLeft(Map.empty[String, Int]) { (acc, k) =>
          acc.updated(k.name, acc.getOrElse(k.name, 0) + 1)
        }
    }

    val analisisIdiomas = IO {
      rows.flatMap(r => Parsear_JSON.parseJsonField[Spoken_Languages](r.getOrElse("spoken_languages", "[]")))
        .foldLeft(Map.empty[String, Int]) { (acc, l) =>
          acc.updated(l.name, acc.getOrElse(l.name, 0) + 1)
        }
    }

    val analisisColecciones = IO {
      rows.flatMap(r => Parsear_JSON.parseJsonFieldSingle[BelongToCollection](r.getOrElse("belongs_to_collection", "{}")).toList)
        .foldLeft(Map.empty[String, Int]) { (acc, c) =>
          acc.updated(c.name, acc.getOrElse(c.name, 0) + 1)
        }
    }

    val analisisCompanias = IO {
      rows.flatMap(r => Parsear_JSON.parseJsonField[Production_Companies](r.getOrElse("production_companies", "[]")))
        .foldLeft(Map.empty[String, Int]) { (acc, c) =>
          acc.updated(c.name, acc.getOrElse(c.name, 0) + 1)
        }
    }

    val analisisPaises = IO {
      rows.flatMap(r => Parsear_JSON.parseJsonField[Production_Countries](r.getOrElse("production_countries", "[]")))
        .foldLeft(Map.empty[String, Int]) { (acc, p) =>
          acc.updated(p.name, acc.getOrElse(p.name, 0) + 1)
        }
    }

    for {
      _ <- mostrarAnalisisJson("GÉNEROS", analisisGeneros)
      _ <- mostrarAnalisisJson("ROLES EN PRODUCCIÓN", analisisRoles)
      _ <- mostrarAnalisisJson("PALABRAS CLAVE", analisisKeywords)
      _ <- mostrarAnalisisJson("IDIOMAS HABLADOS", analisisIdiomas)
      _ <- mostrarAnalisisJson("COLECCIONES", analisisColecciones)
      _ <- mostrarAnalisisJson("COMPAÑÍAS PRODUCTORAS", analisisCompanias)
      _ <- mostrarAnalisisJson("PAÍSES PRODUCTORES", analisisPaises)
    } yield ()
  }

  def analisisfases13a16(
      movies: List[Movie],
      rows: List[Map[String, String]],
      filePath: Path
  ): IO[Unit] = {
    for {
      _ <- IO.println("\n>>> FASE 13: VALIDACIÓN DE IDs")
      count <- IO { rows.count(r => Limpieza.isValidId(r.getOrElse("id", ""))) }
      _ <- printSection(s"Total de películas con ID válido: $count")

      _ <- IO.println("\n>>> FASE 14: ANÁLISIS DE CAST (ACTORES)")
      _ <- analizarCast(movies)

      _ <- IO.println("\n>>> FASE 15: ANÁLISIS DE RATINGS")
      stats <- LecturaJSON.analisisRatings(filePath)
      _ <- printSection("ANÁLISIS COMPLETO DE RATINGS") >>
        IO.println(f"Total de ratings: ${stats.totalRatings}%,d") >>
        IO.println(f"Usuarios únicos: ${stats.usuariosUnicos}%,d") >>
        IO.println(f"Películas con ratings: ${stats.peliculasConRatings}%,d") >>
        printSection("")

      _ <- IO.println("\n>>> FASE 16: ANÁLISIS DE FECHAS DE ESTRENO")
      fechaStats <- LecturaJSON.analizarFechasEstreno(filePath)
      tasaFechas = if (fechaStats.totalFilas > 0)
        fechaStats.fechasValidas.toDouble / fechaStats.totalFilas * 100 else 0.0
      _ <- printSection("ANÁLISIS DE FECHAS DE ESTRENO") >>
        IO.println(f"Fechas válidas: ${fechaStats.fechasValidas}%,d") >>
        IO.println(f"Fechas inválidas: ${fechaStats.fechasInvalidas}%,d") >>
        IO.println(f"Tasa de éxito: $tasaFechas%.2f%%") >>
        printSection("")
    } yield ()
  }

  def analizarCast(movies: List[Movie]): IO[Unit] = {
    val actores = movies.flatMap(m => Parsear_JSON.parseJsonField[Cast](m.cast))
    val actoresPorNombre = actores.foldLeft(Map.empty[String, Int]) { (acc, a) =>
      acc.updated(a.name, acc.getOrElse(a.name, 0) + 1)
    }

    val topActores = actoresPorNombre.toList.sortBy(-_._2).take(15)
    val peliculasConCast = movies.count(m => m.cast.trim != "[]" && m.cast.trim.nonEmpty)
    val promedioActores = if (peliculasConCast > 0) actores.length.toDouble / peliculasConCast else 0.0

    printSection("ESTADÍSTICAS DE CAST (ACTORES)") >>
      IO.println(f"Total de películas con cast: $peliculasConCast%,d") >>
      IO.println(f"Total de actores únicos: ${actoresPorNombre.size}%,d") >>
      IO.println(f"Total de apariciones: ${actores.length}%,d") >>
      IO.println(f"Promedio por película: $promedioActores%.2f") >>
      IO.println("\nTOP 15 ACTORES:") >>
      IO.println("-" * 70) >>
      topActores.zipWithIndex.traverse { case ((actor, count), idx) =>
        IO.println(f"  ${idx + 1}%2d. $actor%-30s: $count%3d películas")
      }.void >>
      printSection("")
  }

  def mostrarAnalisisJson(
      titulo: String,
      ioData: IO[Map[String, Int]],
      topN: Int = 15
  ): IO[Unit] = {
    for {
      datos <- ioData.handleErrorWith { _ => IO.pure(Map.empty[String, Int]) }
      _ <-
        if (datos.isEmpty) {
          IO.println(s"\n>>> FASE: $titulo - No hay datos")
        } else {
          val total = datos.values.sum
          val topItems = datos.toList.sortBy(-_._2).take(topN)

          IO.println(s"\n>>> FASE: $titulo") >>
            printSection(s"TOP $topN - $titulo") >>
            IO.println(f"Total único: ${datos.size}%,d") >>
            IO.println(f"Total de relaciones: $total%,d\n") >>
            topItems.zipWithIndex.traverse { case ((item, count), idx) =>
              val porcentaje = if (total > 0) count.toDouble / total * 100 else 0.0
              IO.println(
                f"  ${idx + 1}%2d. $item%-40s: $count%6d ($porcentaje%5.2f%%)"
              )
            }.void >>
            printSection("")
        }
    } yield ()
  }

  // ============= FUNCIÓN PRINCIPAL =============

  def run: IO[Unit] = {
    ConexionDB.xa.use { transactor =>
      for {
        _ <- printHeader("ANÁLISIS EXPLORATORIO DE DATOS - PELÍCULAS (OPTIMIZADO)")

        results <- LecturaCSV.readMoviesFromCsv(filePath)
        movies = results.collect { case Right(m) => m }
        errors = results.collect { case Left(_) => 1 }.length
        rows <- LecturaCSV.readCsvAsMap(filePath)
        _ <- IO.println(s"✓ ${rows.length} filas cargadas")

        _ <- IO.println("\n>>> FASE 1: CARGA Y LIMPIEZA")
        moviesClean = Limpieza.removeDuplicatesById(movies)
        _ <- IO.println(s"Filas procesadas: ${movies.length}")
        _ <- IO.println(s"Filas con errores: $errors")
        _ <- IO.println(s"Duplicados eliminados: ${movies.length - moviesClean.length}")
        _ <- IO.println(s"Películas finales: ${moviesClean.length}")

        // Ejecutar análisis primero (secuencial)
        _ <- if (!SKIP_ANALYSIS) {
          analisisfase2y3(moviesClean) *>
            analisisfases4a12(rows)
        } else IO.unit
        // Finalmente análisis finales (13-16)
        _ <- if (!SKIP_ANALYSIS) {
          analisisfases13a16(moviesClean, rows, filePath)
        } else IO.unit

        // Luego población BD
        _ <- IO.println("\n>>> FASE 17: POBLACIÓN DE BASE DE DATOS")
        _ <- pipelineOptimizado(transactor, rows)



        _ <- printHeader("PROCESO TERMINADO EXITOSAMENTE")
      } yield ()
    }
  }

  // ============= UTILIDADES =============

  def printHeader(text: String): IO[Unit] =
    IO.println("\n" + "=" * 70) >>
      IO.println(text) >>
      IO.println("=" * 70)

  def printSection(text: String): IO[Unit] =
    IO.println("=" * 70) >>
      IO.println(text) >>
      IO.println("=" * 70)
}
