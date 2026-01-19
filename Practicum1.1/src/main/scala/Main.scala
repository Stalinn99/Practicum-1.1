import cats.effect.{IO, IOApp}
import cats.implicits.*
import fs2.io.file.{Files, Path}
import models.*
import data.*
import fs2.data.csv.decodeUsingHeaders
import utilities.*
import io.circe.generic.auto.*
import fs2.io.file.{Files, Path}
import fs2.data.csv.*
import doobie.implicits.toConnectionIOOps


object Main extends IOApp.Simple {

  val filePath: Path = Path("src/main/resources/data/pi_movies_complete (3) - copia.csv")

    def pipeline(transactor: doobie.Transactor[IO]) =
      Files[IO].readAll(filePath)
        .through(fs2.text.utf8.decode) // Convierte de Stream[Byte] a Stream[String]
        .through(decodeUsingHeaders[Map[String, String]](separator = ';'))
        .zipWithIndex
        .evalMap { case (row, index) =>
          PoblarBaseDatos.populateAll(row)
            .transact(transactor)
            .handleErrorWith { e =>
              PoblarBaseDatos.logError(index, e)
            } *>
            PoblarBaseDatos.logProgress(index, interval = 200)
        }
        .compile.drain
  def run: IO[Unit] = {
    val transactor = ConexionDB.xaSimple
    for {
      _ <- IO.println("\n" + "=" * 70)
      _ <- IO.println("ANÁLISIS EXPLORATORIO DE DATOS - PELÍCULAS (EDA)")
      _ <- IO.println("=" * 70)

      // FASE 1: Carga y Limpieza de Datos
      _ <- IO.println("\n>>> FASE 1: CARGA Y LIMPIEZA DE DATOS")
      results <- LecturaCSV.readMoviesFromCsv(filePath)

      movies = results.collect { case Right(m) => m }
      errors = results.collect { case Left(_) => 1 }.length

      _ <- IO.println(s"Filas procesadas exitosamente: ${movies.length}")
      _ <- IO.println(s"Filas con errores: $errors")

      // Limpieza adicional
      moviesClean = Limpieza.removeDuplicatesById(movies)

      _ <- IO.println(s"Duplicados eliminados: ${movies.length - moviesClean.length}")
      _ <- IO.println(s"Películas antes de limpiar: ${movies.length}")
      _ <- IO.println(s"Películas sin duplicados: ${moviesClean.length}")

      // DEBUG: Ver cuántos votos hay realmente
      _ <- IO.println(s"\n>>> DEBUG VOTOS:")
      _ <- IO.println(s"    Con duplicados - Películas con vote_count > 0: ${movies.count(_.vote_count > 0.0)}")
      _ <- IO.println(s"    Con duplicados - Total de votos: ${movies.filter(_.vote_count > 0.0).map(_.vote_count).sum.toLong}")
      _ <- IO.println(s"    Sin duplicados - Películas con vote_count > 0: ${moviesClean.count(_.vote_count > 0.0)}")
      _ <- IO.println(s"    Sin duplicados - Total de votos: ${moviesClean.filter(_.vote_count > 0.0).map(_.vote_count).sum.toLong}")

      // FASE 2: Análisis Univariable
      _ <- IO.println("\n>>> FASE 2: ANÁLISIS UNIVARIABLE")
      _ <- AnalisisMovie.analyzeMovieStats(movies)

      // FASE 3: Análisis Bivariable
      _ <- IO.println("\n>>> FASE 3: ANÁLISIS BIVARIABLE")
      _ <- AnalisisMovie.analyzeBivariable(movies)

      // FASE 4: Análisis de Géneros
      _ <- IO.println("\n>>> FASE 4: ANÁLISIS DE GÉNEROS")
      _ <- analyzeGenresDistribution()

      // FASE 5: Análisis de Crew
      _ <- IO.println("\n>>> FASE 5: ANÁLISIS DE CREW (ROLES)")
      _ <- analyzeCrewDistribution()

      // FASE 6: Conteo de IDs Válidos
      _ <- IO.println("\n>>> FASE 6: VALIDACIÓN DE IDs")
      _ <- countValidMovies()

      // FASE 7: Análisis de Keywords
      _ <- IO.println("\n>>> FASE 7: PALABRAS CLAVE")
      _ <- analisisKeywords()

      // FASE 8: Análisis de Idiomas
      _ <- IO.println("\n>>> FASE 8: IDIOMAS")
      _ <- mostrarAnalisisIdiomas()

      // FASE 9: Análisis de BelongToCollection
      _ <- IO.println("\n>>> FASE 9: ANÁLISIS DE COLECCIONES")
      _ <- analizarColecciones(movies)

      // FASE 10: Análisis de Cast
      _ <- IO.println("\n>>> FASE 10: ANÁLISIS DE CAST (ACTORES)")
      _ <- analizarCast(movies)

      // FASE 11: Análisis de Production Companies
      _ <- IO.println("\n>>> FASE 11: ANÁLISIS DE COMPAÑÍAS PRODUCTORAS")
      _ <- analizarCompaniasProductoras(movies)

      // FASE 12: Análisis de Production Countries
      _ <- IO.println("\n>>> FASE 12: ANÁLISIS DE PAÍSES PRODUCTORES")
      _ <- analizarPaisesProductores(movies)

      // FASE 13: Análisis de Rating
      _ <- IO.println("\n>>> FASE 13: ANÁLISIS DE RATINGS")
      _ <- analizarRatings(movies)

      // FASE 14: POBLACIÓN
      _ <- IO.println("\n>>> FASE 14: POBLACIÓN DE BASE DE DATOS")
      _ <- IO.println(" Iniciando proceso de carga...")
      _ <- pipeline(transactor) // Ahora 'transactor' ya existe en este contexto

      _ <- IO.println("=" * 70)
      _ <- IO.println(" PROCESO TERMINADO EXITOSAMENTE")
      _ <- IO.println("=" * 70)
    } yield ()
  }

  def analizarColecciones(movies: List[Movie]): IO[Unit] = {
    try {
      val colecciones: Map[String, Int] = movies
        .filter { m =>
          m.belongs_to_collection.trim != "null" &&
            m.belongs_to_collection.trim != "{}" &&
            m.belongs_to_collection.trim.nonEmpty
        }
        .flatMap { movie =>
          try {
            val parsed = Parsear_JSON.parseJsonFieldSingle[BelongToCollection](movie.belongs_to_collection)
            parsed.map(_.name).toList
          } catch {
            case _: Exception => List.empty[String]
          }
        }
        .groupBy(identity)
        .view.mapValues(_.length)
        .toMap

      val totalPeliculasEnColecciones = colecciones.values.sum
      val peliculasSinColeccion = movies.length - totalPeliculasEnColecciones
      val topColecciones = Estadistico.topN(colecciones, 15)

      IO.println("=" * 70) >>
        IO.println("PELÍCULAS POR COLECCIÓN") >>
        IO.println("=" * 70) >>
        IO.println(f"Total de películas en colecciones:    $totalPeliculasEnColecciones%,d (${totalPeliculasEnColecciones * 100.0 / movies.length}%.2f%%)") >>
        IO.println(f"Películas sin colección asignada:     $peliculasSinColeccion%,d (${peliculasSinColeccion * 100.0 / movies.length}%.2f%%)") >>
        IO.println(f"Total de colecciones únicas:          ${colecciones.size}") >>
        IO.println("\nTOP 15 COLECCIONES CON MÁS PELÍCULAS:") >>
        IO.println("-" * 70) >>
        topColecciones.zipWithIndex.traverse { case ((coleccion, count), idx) =>
          val porcentaje = (count.toDouble / totalPeliculasEnColecciones * 100)
          IO.println(f"  ${idx + 1}%2d. $coleccion%-40s: $count%3d películas ($porcentaje%5.2f%%)")
        }.void >>
        IO.println("=" * 70)
    } catch {
      case e: Exception =>
        IO.println(s"Error en análisis de colecciones: ${e.getMessage}") >>
          IO.println("=" * 70)
    }
  }
  def analizarCast(movies: List[Movie]): IO[Unit] = {
    try {
      val actores: List[Cast] = movies
        .flatMap { movie =>
          Parsear_JSON.parseJsonField[Cast](movie.cast)
        }

      // Análisis por actor
      val actoresPorNombre: Map[String, Int] = actores
        .map(_.name)
        .groupBy(identity)
        .view.mapValues(_.length)
        .toMap

      // Análisis de personajes por actor (actores con múltiples personajes)
      val actoresMultiplePersonajes: List[(String, Int)] = actoresPorNombre
        .filter(_._2 > 1)
        .toList
        .sortBy(-_._2)
        .take(20)

      val totalActores = actoresPorNombre.size
      val peliculasConCast = movies.count(c => c.cast.trim != "[]" && c.cast.trim.nonEmpty)
      val promedioActoresPorPelicula = if (peliculasConCast > 0) actores.length.toDouble / peliculasConCast else 0.0

      IO.println("=" * 70) >>
        IO.println("ESTADÍSTICAS DE CAST (ACTORES)") >>
        IO.println("=" * 70) >>
        IO.println(f"Total de películas con cast:              $peliculasConCast%,d") >>
        IO.println(f"Total de actores únicos:                  $totalActores%,d") >>
        IO.println(f"Total de apariciones de actores:          ${actores.length}%,d") >>
        IO.println(f"Promedio de actores por película:         $promedioActoresPorPelicula%.2f") >>
        IO.println("\nTOP 20 ACTORES CON MÁS PELÍCULAS:") >>
        IO.println("-" * 70) >>
        Estadistico.topN(actoresPorNombre, 20).zipWithIndex.traverse { case ((actor, count), idx) =>
          IO.println(f"  ${idx + 1}%2d. $actor%-30s: $count%3d películas")
        }.void >>
        IO.println("\nACTORES CON MÚLTIPLES PERSONAJES (en la misma película):") >>
        IO.println("-" * 70) >>
        (if (actoresMultiplePersonajes.nonEmpty) {
          actoresMultiplePersonajes.zipWithIndex.traverse { case ((actor, count), idx) =>
            IO.println(f"  ${idx + 1}%2d. $actor%-30s: $count%2d personajes")
          }.void
        } else {
          IO.println("  (No se encontraron actores con múltiples personajes)")
        }) >>
        IO.println("=" * 70)
    } catch {
      case e: Exception =>
        IO.println(s"Error en análisis de cast: ${e.getMessage}") >>
          IO.println("=" * 70)
    }
  }
  def analizarCompaniasProductoras(movies: List[Movie]): IO[Unit] = {
    try {
      // Extraer todas las compañías
      val companias: List[Production_Companies] = movies
        .flatMap { movie =>
          Parsear_JSON.parseJsonField[Production_Companies](movie.production_companies)
        }

      // Contar películas por compañía
      val peliculasPorCompania: Map[String, Int] = companias
        .map(_.name)
        .groupBy(identity)
        .view.mapValues(_.length)
        .toMap

      val totalCompanias = peliculasPorCompania.size
      val peliculasConProducer = movies.count(pdcmp => pdcmp.production_companies.trim != "[]" && pdcmp.production_companies.trim.nonEmpty)
      val top5Companias = Estadistico.topN(peliculasPorCompania, 5)

      IO.println("=" * 70) >>
        IO.println("TOP 5 COMPAÑÍAS PRODUCTORAS") >>
        IO.println("=" * 70) >>
        IO.println(f"Total de películas con productor:        $peliculasConProducer%,d (${peliculasConProducer * 100.0 / movies.length}%.2f%%)") >>
        IO.println(f"Total de compañías productoras únicas:    $totalCompanias%,d") >>
        IO.println(f"Total de relaciones película-compañía:    ${companias.length}%,d") >>
        IO.println("\nTOP 5 COMPAÑÍAS CON MÁS PELÍCULAS:") >>
        IO.println("-" * 70) >>
        top5Companias.zipWithIndex.traverse { case ((compania, count), idx) =>
          val porcentaje = (count.toDouble / companias.length * 100)
          IO.println(f"  ${idx + 1}. $compania%-45s: $count%3d películas ($porcentaje%5.2f%%)")
        }.void >>
        IO.println("\nTOP 15 COMPAÑÍAS PRODUCTORAS:") >>
        IO.println("-" * 70) >>
        Estadistico.topN(peliculasPorCompania, 15).zipWithIndex.traverse { case ((compania, count), idx) =>
          val porcentaje = (count.toDouble / companias.length * 100)
          IO.println(f"  ${idx + 1}%2d. $compania%-40s: $count%3d ($porcentaje%5.2f%%)")
        }.void >>
        IO.println("=" * 70)
    } catch {
      case e: Exception =>
        IO.println(s"Error en análisis de compañías productoras: ${e.getMessage}") >>
          IO.println("=" * 70)
    }
  }

  def analizarPaisesProductores(movies: List[Movie]): IO[Unit] = {
    try {
      // Extraer todos los países
      val paises: List[Production_Countries] = movies
        .flatMap { movie =>
          Parsear_JSON.parseJsonField[Production_Countries](movie.production_countries)
        }

      // Contar películas por país
      val peliculasPorPais: Map[String, Int] = paises
        .map(_.name)
        .groupBy(identity)
        .view.mapValues(_.length)
        .toMap

      val totalPaises = peliculasPorPais.size
      val peliculasConPais = movies.count(pdcts => pdcts.production_countries.trim != "[]" && pdcts.production_countries.trim.nonEmpty)
      val top5Paises = Estadistico.topN(peliculasPorPais, 5)

      IO.println("=" * 70) >>
        IO.println("TOP 5 PAÍSES PRODUCTORES") >>
        IO.println("=" * 70) >>
        IO.println(f"Total de películas con país de origen:    $peliculasConPais%,d (${peliculasConPais * 100.0 / movies.length}%.2f%%)") >>
        IO.println(f"Total de países únicos:                   $totalPaises%,d") >>
        IO.println(f"Total de relaciones película-país:        ${paises.length}%,d") >>
        IO.println("\nTOP 5 PAÍSES CON MÁS PELÍCULAS:") >>
        IO.println("-" * 70) >>
        top5Paises.zipWithIndex.traverse { case ((pais, count), idx) =>
          val porcentaje = (count.toDouble / paises.length * 100)
          IO.println(f"  ${idx + 1}. $pais%-40s: $count%3d películas ($porcentaje%5.2f%%)")
        }.void >>
        IO.println("\nTOP 15 PAÍSES PRODUCTORES:") >>
        IO.println("-" * 70) >>
        Estadistico.topN(peliculasPorPais, 15).zipWithIndex.traverse { case ((pais, count), idx) =>
          val porcentaje = (count.toDouble / paises.length * 100)
          IO.println(f"  ${idx + 1}%2d. $pais%-40s: $count%3d ($porcentaje%5.2f%%)")
        }.void >>
        IO.println("=" * 70)
    } catch {
      case e: Exception =>
        IO.println(s"Error en análisis de países productores: ${e.getMessage}") >>
          IO.println("=" * 70)
    }
  }

  def analizarRatings(movies: List[Movie]): IO[Unit] = {
    try {
      // Crear una lista simulada de ratings basada en vote_count y vote_average
      val ratingsData: List[(Double, Double)] = movies
        .filter { m =>
          m.vote_count > 0.0 && m.vote_average > 0.0
        }
        .map(m => (m.vote_average, m.vote_count))

      val totalRatings = ratingsData.map(_._2).sum.toLong
      val usuariosUnicos = ratingsData.length
      val promedioVotos = if (usuariosUnicos > 0) ratingsData.map(_._2).sum / usuariosUnicos else 0.0

      // Estadísticas de calificaciones
      val calificaciones = ratingsData.map(_._1)
      val statsRatings = Estadistico.calculateStats(calificaciones)

      // Distribución por rango de calificación
      val distribucionPorRango: Map[String, Int] = ratingsData.map(_._1).groupBy { rating =>
        if (rating >= 8.0) "Excelente (8.0-10.0)"
        else if (rating >= 6.0) "Buena (6.0-7.9)"
        else if (rating >= 4.0) "Regular (4.0-5.9)"
        else "Pobre (0.0-3.9)"
      }.view.mapValues(_.length).toMap

      IO.println("=" * 70) >>
        IO.println("ANÁLISIS DE RATINGS Y VOTOS") >>
        IO.println("=" * 70) >>
        IO.println(f"Total de películas con votos:             ${ratingsData.length}%,d") >>
        IO.println(f"Total de votos/ratings:                   $totalRatings%,d") >>
        IO.println(f"Promedio de votos por película:           $promedioVotos%.2f") >>
        IO.println("\nESTADÍSTICAS DE CALIFICACIÓN (Vote Average):") >>
        IO.println("-" * 70) >>
        AnalisisMovie.printNumericStats(statsRatings) >>
        IO.println("\nDISTRIBUCIÓN DE PELÍCULAS POR RANGO DE CALIFICACIÓN:") >>
        IO.println("-" * 70) >>
        distribucionPorRango.toList.sortBy(-_._2).traverse { case (rango, count) =>
          val porcentaje = (count.toDouble / usuariosUnicos * 100)
          IO.println(f"  $rango%-30s: $count%6d películas ($porcentaje%5.2f%%)")
        }.void >>
        IO.println("=" * 70)
    } catch {
      case e: Exception =>
        IO.println(s"Error en análisis de ratings: ${e.getMessage}") >>
          IO.println("=" * 70)
    }
  }
  def analyzeGenresDistribution(): IO[Unit] = {
    for {
      genreFreq <- LecturaJSON.analyzeGenres(filePath)

      _ <- if (genreFreq.isEmpty) {
        IO.println("No se encontraron datos de géneros")
      } else {
        val total = genreFreq.values.sum
        val topGenres = Estadistico.topN(genreFreq, 10)

        IO.println("=" * 70) >>
          IO.println("TOP 10 GÉNEROS MÁS FRECUENTES") >>
          IO.println("=" * 70) >>
          topGenres.zipWithIndex.traverse { case ((genre, count), idx) =>
            val percentage = (count.toDouble / total * 100)
            IO.println(f"  ${idx + 1}%2d. $genre%-25s: $count%6d ($percentage%5.2f%%)")
          }.void >>
          IO.println("=" * 70)
      }
    } yield ()
  }

  def analyzeCrewDistribution(): IO[Unit] = {
    for {
      jobFreq: Map[String, Int] <- LecturaJSON.analyzeCrewByJob(filePath)

      _ <- if (jobFreq.isEmpty) {
        IO.println("No se encontraron datos de crew")
      } else {
        val total = jobFreq.values.sum
        val topJobs = Estadistico.topN(jobFreq, 15)

        IO.println("=" * 70) >>
          IO.println("TOP 15 ROLES MÁS FRECUENTES EN PRODUCCIÓN") >>
          IO.println("=" * 70) >>
          topJobs.zipWithIndex.traverse { case ((job, count), idx) =>
            val percentage: Double = count.toDouble / total * 100
            IO.println(f"${idx + 1}%2d. $job%-35s: $count%6d ($percentage%5.2f%%)")
          }.void >>
          IO.println("=" * 70)
      }
    } yield ()
  }

  def countValidMovies(): IO[Unit] = {
    for {
      count <- LecturaCSV.countValidRows(
        filePath,
        "id",
        Limpieza.isValidId
      )

      _ <- IO.println("=" * 70) >>
        IO.println("VALIDACIÓN DE IDs") >>
        IO.println("=" * 70) >>
        IO.println(f"Total de películas con ID válido: $count") >>
        IO.println("=" * 70)
    } yield ()
  }

  def analisisKeywords(): IO[Unit] = {
    for {
      wordsFreq <- LecturaJSON.analisisKeyWords(filePath).handleErrorWith { error =>
        IO.println(s"Error al leer keywords: ${error.getMessage}") *>
          IO.pure(Map.empty[String, Int])
      }
      _ <- if (wordsFreq.isEmpty) {
        IO.println("No se encontraron datos en la columna keywords")
      } else {
        val total: Int = wordsFreq.values.sum
        val topWords: List[(String, Int)] = Estadistico.topN(wordsFreq, 10)
        IO.println("=" * 70) >>
          IO.println("TOP 10 PALABRAS CLAVE MÁS FRECUENTES") >>
          IO.println("=" * 70) >>
          IO.println(f"Total de keywords: $total%,d\n") >>
          topWords.zipWithIndex.traverse { case ((word, count), idx) =>
            val porcentaje: Double = (count.toDouble / total) * 100
            IO.println(f"  ${idx + 1}%2d. $word%-35s: $count%6d ($porcentaje%5.2f%%)")
          }.void >>
          IO.println("=" * 70)
      }
    } yield ()
  }

  def mostrarAnalisisIdiomas(): IO[Unit] = {
    for {
      langFreq <- LecturaJSON.analisisSpokenLenguaje(filePath).handleErrorWith { error =>
        IO.println(s"Error al leer idiomas: ${error.getMessage}") *>
          IO.pure(Map.empty[String, Int])
      }

      _ <- if (langFreq.isEmpty) {
        IO.println("No se encontraron datos de idiomas")
      } else {
        val total = langFreq.values.sum
        val topLangs = Estadistico.topN(langFreq, 10)

        IO.println("=" * 70) >>
          IO.println("TOP 10 IDIOMAS MÁS PRESENTES") >>
          IO.println("=" * 70) >>
          topLangs.zipWithIndex.traverse { case ((lang, count), idx) =>
            val percentage = (count.toDouble / total * 100)
            IO.println(f"  ${idx + 1}%2d. $lang%-25s: $count%6d ($percentage%5.2f%%)")
          }.void >>
          IO.println("=" * 70)
      }
    } yield ()
  }
}