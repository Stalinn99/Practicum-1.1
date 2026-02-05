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
  val BATCH_SIZE: Int = 2000
  val SKIP_ANALYSIS: Boolean = false
  val DISABLE_FK_CHECKS: Boolean = true

  // ============= PIPELINE OPTIMIZADO =============

  def pipelineOptimizado(
                          transactor: doobie.Transactor[IO],
                          rows: List[Map[String, String]]
                        ): IO[Unit] = {
    for {
      _ <- if (DISABLE_FK_CHECKS)
        sql"SET FOREIGN_KEY_CHECKS=0".update.run.transact(transactor)
      else IO.unit

      batches: List[List[Map[String, String]]] = rows.grouped(BATCH_SIZE).toList
      _ <- IO.println(s"Total de lotes: ${batches.length}")
      _ <- procesarLotes(transactor, batches)

      _ <- if (DISABLE_FK_CHECKS)
        sql"SET FOREIGN_KEY_CHECKS=1".update.run.transact(transactor)
      else IO.unit

      _ <- IO.println("Población de BD completada")
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
          res <- PoblarBaseDatos.populateBatch(batch).transact(transactor).attempt
          _ <- res match {
            case Left(e) =>
              IO.println(s"ERROR Batch $idx: ${e.getMessage}")
            case Right(_) =>
              val porcentaje:Double = (idx + 1) * 100 / batches.length
              IO.println(s"Batch ${idx + 1}/${batches.length} procesado | ${batch.size} filas | $porcentaje% completado")
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

  // ============= Usa funciones de LecturaJSON =============
  def analisisfases4a12(): IO[Unit] = {
    for {
      // FASE 4: GÉNEROS
      analisisGeneros <- LecturaJSON.analyzeGenres(filePath)
      _ <- mostrarAnalisisJsonData("GÉNEROS", analisisGeneros)

      // FASE 5: ROLES EN PRODUCCIÓN
      analisisRoles <- LecturaJSON.analyzeCrewByJob(filePath)
      _ <- mostrarAnalisisJsonData("ROLES EN PRODUCCIÓN", analisisRoles)

      // FASE 6: DEPARTAMENTOS DE PRODUCCIÓN
      analisisDepartamentos <- LecturaJSON.analyzeCrewByDepartment(filePath)
      _ <- mostrarAnalisisJsonData("DEPARTAMENTOS DE PRODUCCIÓN", analisisDepartamentos)

      // FASE 7: PALABRAS CLAVE
      analisisKeywords <- LecturaJSON.analisisKeyWords(filePath)
      _ <- mostrarAnalisisJsonData("PALABRAS CLAVE", analisisKeywords)

      // FASE 8: IDIOMAS HABLADOS
      analisisIdiomas <- LecturaJSON.analisisSpokenLenguaje(filePath)
      _ <- mostrarAnalisisJsonData("IDIOMAS HABLADOS", analisisIdiomas)

      // FASE 9: COLECCIONES
      analisisColecciones <- LecturaJSON.analisisColecciones(filePath)
      _ <- mostrarAnalisisJsonData("COLECCIONES", analisisColecciones)

      // FASE 10: COMPAÑÍAS PRODUCTORAS
      analisisCompanias <- LecturaJSON.analisisCompanias(filePath)
      _ <- mostrarAnalisisJsonData("COMPAÑÍAS PRODUCTORAS", analisisCompanias)

      // FASE 11: PAÍSES PRODUCTORES
      analisisPaises <- LecturaJSON.analisisPaises(filePath)
      _ <- mostrarAnalisisJsonData("PAÍSES PRODUCTORES", analisisPaises)

    } yield ()
  }

  def analisisfases13a16(
                          movies: List[Movie],
                          rows: List[Map[String, String]],
                          filePath: Path
                        ): IO[Unit] = {
    for {
      _ <- IO.println("\n>>> FASE 13: VALIDACIÓN DE IDs")
      count <- LecturaCSV.countValidRows(filePath, "id", Limpieza.isValidId)
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

  // ============= MOSTRAR ANÁLISIS =============
  def mostrarAnalisisJsonData(
                               titulo: String,
                               datos: Map[String, Int],
                               topN: Int = 15
                             ): IO[Unit] = {
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
  }

  // ============= FUNCIÓN PRINCIPAL =============

  def run: IO[Unit] = {
    ConexionDB.xa.use { transactor =>
      for {
        _ <- printHeader("ANÁLISIS EXPLORATORIO DE DATOS - PELÍCULAS (COMPLETO)")

        results <- LecturaCSV.readMoviesFromCsv(filePath)
        movies = results.collect { case Right(m) => m }
        errors = results.collect { case Left(_) => 1 }.length
        rows <- LecturaCSV.readCsvAsMap(filePath)
        _ <- IO.println(s"${rows.length} filas cargadas")

        _ <- IO.println("\n>>> FASE: CARGA Y LIMPIEZA")
        moviesClean = Limpieza.removeDuplicatesById(movies)
        _ <- IO.println(s"Filas procesadas: ${movies.length}")
        _ <- IO.println(s"Filas con errores: $errors")
        _ <- IO.println(s"Duplicados eliminados: ${movies.length - moviesClean.length}")
        _ <- IO.println(s"Películas finales: ${moviesClean.length}")

        // Ejecutar análisis primero (secuencial)
        _ <- if (!SKIP_ANALYSIS) {
          analisisfase2y3(moviesClean) *>
            analisisfases4a12()
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
