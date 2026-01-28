error id: 102D197E4BA1A9B7E6FB5B8D07BB0A78
file:///C:/Users/Lenin/Desktop/Practicum-1.1/Practicum1.1/src/main/scala/Main.scala
### java.lang.AssertionError: assertion failed

occurred in the presentation compiler.



action parameters:
offset: 9877
uri: file:///C:/Users/Lenin/Desktop/Practicum-1.1/Practicum1.1/src/main/scala/Main.scala
text:
```scala
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
              val porcentaje = (idx + 1) * 100 / batches.length
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
        _ <- if (!SKIP_ANALYS@@IS) {
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
```


presentation compiler configuration:
Scala version: 3.3.3
Classpath:
<WORKSPACE>\.bloop\root\bloop-bsp-clients-classes\classes-Metals-Ju_LUYBETpiwzerk9kNOVQ== [exists ], <HOME>\AppData\Local\bloop\cache\semanticdb\com.sourcegraph.semanticdb-javac.0.11.2\semanticdb-javac-0.11.2.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\scala-lang\scala3-library_3\3.3.3\scala3-library_3-3.3.3.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\cats-effect_3\3.5.3\cats-effect_3-3.5.3.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\co\fs2\fs2-core_3\3.9.3\fs2-core_3-3.9.3.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\co\fs2\fs2-io_3\3.9.3\fs2-io_3-3.9.3.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\gnieh\fs2-data-csv_3\1.9.1\fs2-data-csv_3-1.9.1.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\gnieh\fs2-data-csv-generic_3\1.9.1\fs2-data-csv-generic_3-1.9.1.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\tpolecat\doobie-core_3\1.0.0-RC4\doobie-core_3-1.0.0-RC4.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\tpolecat\doobie-hikari_3\1.0.0-RC4\doobie-hikari_3-1.0.0-RC4.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\io\circe\circe-core_3\0.14.6\circe-core_3-0.14.6.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\io\circe\circe-generic_3\0.14.6\circe-generic_3-0.14.6.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\io\circe\circe-parser_3\0.14.6\circe-parser_3-0.14.6.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\scala-lang\scala-library\2.13.12\scala-library-2.13.12.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\cats-effect-kernel_3\3.5.3\cats-effect-kernel_3-3.5.3.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\cats-effect-std_3\3.5.3\cats-effect-std_3-3.5.3.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\scodec\scodec-bits_3\1.1.38\scodec-bits_3-1.1.38.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\cats-core_3\2.10.0\cats-core_3-2.10.0.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\com\comcast\ip4s-core_3\3.4.0\ip4s-core_3-3.4.0.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\gnieh\fs2-data-text_3\1.9.1\fs2-data-text_3-1.9.1.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\scala-lang\modules\scala-collection-compat_3\2.11.0\scala-collection-compat_3-2.11.0.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\portable-scala\portable-scala-reflect_2.13\1.1.2\portable-scala-reflect_2.13-1.1.2.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\shapeless3-deriving_3\3.3.0\shapeless3-deriving_3-3.3.0.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\tpolecat\doobie-free_3\1.0.0-RC4\doobie-free_3-1.0.0-RC4.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\tpolecat\typename_3\1.1.0\typename_3-1.1.0.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\com\zaxxer\HikariCP\5.0.1\HikariCP-5.0.1.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\io\circe\circe-numbers_3\0.14.6\circe-numbers_3-0.14.6.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\io\circe\circe-jawn_3\0.14.6\circe-jawn_3-0.14.6.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\cats-kernel_3\2.10.0\cats-kernel_3-2.10.0.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\literally_3\1.1.0\literally_3-1.1.0.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\cats-free_3\2.9.0\cats-free_3-2.9.0.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\com\google\protobuf\protobuf-java\3.21.9\protobuf-java-3.21.9.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\jawn-parser_3\1.4.0\jawn-parser_3-1.4.0.jar [exists ]
Options:
-Xsemanticdb -sourceroot <WORKSPACE> -release 21




#### Error stacktrace:

```
scala.runtime.Scala3RunTime$.assertFailed(Scala3RunTime.scala:11)
	dotty.tools.dotc.core.TypeOps$.dominators$1(TypeOps.scala:248)
	dotty.tools.dotc.core.TypeOps$.approximateOr$1(TypeOps.scala:382)
	dotty.tools.dotc.core.TypeOps$.orDominator(TypeOps.scala:395)
	dotty.tools.dotc.core.Types$OrType.join(Types.scala:3468)
	dotty.tools.dotc.core.Types$OrType.widenUnionWithoutNull(Types.scala:3484)
	dotty.tools.dotc.core.Types$Type.widenUnion(Types.scala:1322)
	dotty.tools.dotc.core.ConstraintHandling.widenOr$1(ConstraintHandling.scala:652)
	dotty.tools.dotc.core.ConstraintHandling.widenInferred(ConstraintHandling.scala:668)
	dotty.tools.dotc.core.ConstraintHandling.widenInferred$(ConstraintHandling.scala:29)
	dotty.tools.dotc.core.TypeComparer.widenInferred(TypeComparer.scala:30)
	dotty.tools.dotc.core.ConstraintHandling.instanceType(ConstraintHandling.scala:707)
	dotty.tools.dotc.core.ConstraintHandling.instanceType$(ConstraintHandling.scala:29)
	dotty.tools.dotc.core.TypeComparer.instanceType(TypeComparer.scala:30)
	dotty.tools.dotc.core.TypeComparer$.instanceType(TypeComparer.scala:3012)
	dotty.tools.dotc.core.Types$TypeVar.instantiate(Types.scala:4846)
	dotty.tools.dotc.typer.Inferencing.tryInstantiate$1(Inferencing.scala:731)
	dotty.tools.dotc.typer.Inferencing.doInstantiate$1(Inferencing.scala:734)
	dotty.tools.dotc.typer.Inferencing.interpolateTypeVars(Inferencing.scala:737)
	dotty.tools.dotc.typer.Inferencing.interpolateTypeVars$(Inferencing.scala:552)
	dotty.tools.dotc.typer.Typer.interpolateTypeVars(Typer.scala:117)
	dotty.tools.dotc.typer.Typer.simplify(Typer.scala:3131)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3117)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.typedTuple(Typer.scala:2959)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3087)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3115)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3303)
	dotty.tools.dotc.typer.Typer.typedBlock(Typer.scala:1168)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3058)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3115)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3303)
	dotty.tools.dotc.typer.Typer.caseRest$1(Typer.scala:1874)
	dotty.tools.dotc.typer.Typer.typedCase(Typer.scala:1890)
	dotty.tools.dotc.typer.Typer.typedCases$$anonfun$1(Typer.scala:1820)
	dotty.tools.dotc.core.Decorators$.loop$1(Decorators.scala:94)
	dotty.tools.dotc.core.Decorators$.mapconserve(Decorators.scala:110)
	dotty.tools.dotc.typer.Typer.typedCases(Typer.scala:1822)
	dotty.tools.dotc.typer.Typer.$anonfun$34(Typer.scala:1812)
	dotty.tools.dotc.typer.Applications.harmonic(Applications.scala:2333)
	dotty.tools.dotc.typer.Applications.harmonic$(Applications.scala:352)
	dotty.tools.dotc.typer.Typer.harmonic(Typer.scala:117)
	dotty.tools.dotc.typer.Typer.typedMatchFinish(Typer.scala:1812)
	dotty.tools.dotc.typer.Typer.typedMatch(Typer.scala:1746)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3064)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3115)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3303)
	dotty.tools.dotc.typer.Namer.typedAheadExpr$$anonfun$1(Namer.scala:1656)
	dotty.tools.dotc.typer.Namer.typedAhead(Namer.scala:1646)
	dotty.tools.dotc.typer.Namer.typedAheadExpr(Namer.scala:1656)
	dotty.tools.dotc.typer.Namer.valOrDefDefSig(Namer.scala:1712)
	dotty.tools.dotc.typer.Namer.defDefSig(Namer.scala:1792)
	dotty.tools.dotc.typer.Namer$Completer.typeSig(Namer.scala:791)
	dotty.tools.dotc.typer.Namer$Completer.completeInCreationContext(Namer.scala:934)
	dotty.tools.dotc.typer.Namer$Completer.complete(Namer.scala:814)
	dotty.tools.dotc.core.SymDenotations$SymDenotation.completeFrom(SymDenotations.scala:174)
	dotty.tools.dotc.core.Denotations$Denotation.completeInfo$1(Denotations.scala:187)
	dotty.tools.dotc.core.Denotations$Denotation.info(Denotations.scala:189)
	dotty.tools.dotc.core.SymDenotations$SymDenotation.ensureCompleted(SymDenotations.scala:393)
	dotty.tools.dotc.typer.Typer.retrieveSym(Typer.scala:2991)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3016)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3114)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3213)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3259)
	dotty.tools.dotc.typer.Typer.typedBlockStats(Typer.scala:1161)
	dotty.tools.dotc.typer.Typer.typedBlock(Typer.scala:1165)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3058)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3115)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.typedFunctionValue(Typer.scala:1630)
	dotty.tools.dotc.typer.Typer.typedFunction(Typer.scala:1381)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3060)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3115)
	dotty.tools.dotc.typer.ProtoTypes$FunProto.$anonfun$7(ProtoTypes.scala:495)
	dotty.tools.dotc.typer.ProtoTypes$FunProto.cacheTypedArg(ProtoTypes.scala:418)
	dotty.tools.dotc.typer.ProtoTypes$FunProto.typedArg(ProtoTypes.scala:496)
	dotty.tools.dotc.typer.Applications$ApplyToUntyped.typedArg(Applications.scala:897)
	dotty.tools.dotc.typer.Applications$ApplyToUntyped.typedArg(Applications.scala:897)
	dotty.tools.dotc.typer.Applications$Application.addTyped$1(Applications.scala:589)
	dotty.tools.dotc.typer.Applications$Application.matchArgs(Applications.scala:653)
	dotty.tools.dotc.typer.Applications$Application.init(Applications.scala:492)
	dotty.tools.dotc.typer.Applications$TypedApply.<init>(Applications.scala:779)
	dotty.tools.dotc.typer.Applications$ApplyToUntyped.<init>(Applications.scala:896)
	dotty.tools.dotc.typer.Applications.ApplyTo(Applications.scala:1126)
	dotty.tools.dotc.typer.Applications.ApplyTo$(Applications.scala:352)
	dotty.tools.dotc.typer.Typer.ApplyTo(Typer.scala:117)
	dotty.tools.dotc.typer.Applications.simpleApply$1(Applications.scala:969)
	dotty.tools.dotc.typer.Applications.realApply$1$$anonfun$2(Applications.scala:1052)
	dotty.tools.dotc.typer.Typer.tryEither(Typer.scala:3327)
	dotty.tools.dotc.typer.Applications.realApply$1(Applications.scala:1063)
	dotty.tools.dotc.typer.Applications.typedApply(Applications.scala:1101)
	dotty.tools.dotc.typer.Applications.typedApply$(Applications.scala:352)
	dotty.tools.dotc.typer.Typer.typedApply(Typer.scala:117)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3050)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3115)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3303)
	dotty.tools.dotc.typer.Typer.typeSelectOnTerm$1(Typer.scala:755)
	dotty.tools.dotc.typer.Typer.typedSelect(Typer.scala:793)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3019)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3114)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3303)
	dotty.tools.dotc.typer.Applications.realApply$1(Applications.scala:941)
	dotty.tools.dotc.typer.Applications.typedApply(Applications.scala:1101)
	dotty.tools.dotc.typer.Applications.typedApply$(Applications.scala:352)
	dotty.tools.dotc.typer.Typer.typedApply(Typer.scala:117)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3050)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3115)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3098)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3115)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3303)
	dotty.tools.dotc.typer.Typer.typedBlock(Typer.scala:1168)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3058)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3115)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3303)
	dotty.tools.dotc.typer.Typer.$anonfun$57(Typer.scala:2486)
	dotty.tools.dotc.inlines.PrepareInlineable$.dropInlineIfError(PrepareInlineable.scala:243)
	dotty.tools.dotc.typer.Typer.typedDefDef(Typer.scala:2486)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3026)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3114)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3213)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3259)
	dotty.tools.dotc.typer.Typer.typedClassDef(Typer.scala:2669)
	dotty.tools.dotc.typer.Typer.typedTypeOrClassDef$1(Typer.scala:3038)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3042)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3114)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3213)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3259)
	dotty.tools.dotc.typer.Typer.typedPackageDef(Typer.scala:2812)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3083)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3115)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3303)
	dotty.tools.dotc.typer.TyperPhase.typeCheck$$anonfun$1(TyperPhase.scala:44)
	dotty.tools.dotc.typer.TyperPhase.typeCheck$$anonfun$adapted$1(TyperPhase.scala:50)
	scala.Function0.apply$mcV$sp(Function0.scala:42)
	dotty.tools.dotc.core.Phases$Phase.monitor(Phases.scala:440)
	dotty.tools.dotc.typer.TyperPhase.typeCheck(TyperPhase.scala:50)
	dotty.tools.dotc.typer.TyperPhase.runOn$$anonfun$3(TyperPhase.scala:84)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.typer.TyperPhase.runOn(TyperPhase.scala:84)
	dotty.tools.dotc.Run.runPhases$1$$anonfun$1(Run.scala:246)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.ArrayOps$.foreach$extension(ArrayOps.scala:1323)
	dotty.tools.dotc.Run.runPhases$1(Run.scala:262)
	dotty.tools.dotc.Run.compileUnits$$anonfun$1(Run.scala:270)
	dotty.tools.dotc.Run.compileUnits$$anonfun$adapted$1(Run.scala:279)
	dotty.tools.dotc.util.Stats$.maybeMonitored(Stats.scala:71)
	dotty.tools.dotc.Run.compileUnits(Run.scala:279)
	dotty.tools.dotc.Run.compileSources(Run.scala:194)
	dotty.tools.dotc.interactive.InteractiveDriver.run(InteractiveDriver.scala:165)
	scala.meta.internal.pc.MetalsDriver.run(MetalsDriver.scala:45)
	scala.meta.internal.pc.WithCompilationUnit.<init>(WithCompilationUnit.scala:28)
	scala.meta.internal.pc.WithSymbolSearchCollector.<init>(PcCollector.scala:362)
	scala.meta.internal.pc.PcDocumentHighlightProvider.<init>(PcDocumentHighlightProvider.scala:16)
	scala.meta.internal.pc.ScalaPresentationCompiler.documentHighlight$$anonfun$1(ScalaPresentationCompiler.scala:178)
```
#### Short summary: 

java.lang.AssertionError: assertion failed