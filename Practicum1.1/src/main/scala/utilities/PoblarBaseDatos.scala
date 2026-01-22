package utilities

import cats.effect.IO
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.free.connection as FC
import io.circe.generic.auto.deriveDecoder
import models.*
import utilities.Parsear_JSON.*

object PoblarBaseDatos {

  /** Helper para conversiones seguras */
  def safeInt(s: String): Int = s.trim.toDoubleOption.map(_.toInt).getOrElse(0)
  def safeDouble(s: String): Double = s.trim.toDoubleOption.getOrElse(0.0)

  /**
   * Puebla TODAS las tablas relacionadas con una película.
   * Utiliza transacciones composables (ConnectionIO).
   */
  def populateAll(row: Map[String, String]): ConnectionIO[Unit] = {
    val pId = safeInt(row.getOrElse("id", "0"))
    val origLang = row.getOrElse("original_language", "un").trim.take(2)

    // Si el ID es inválido (0), saltamos esta fila
    if (pId == 0) return FC.unit

    // Definimos las operaciones SQL
    val insertIdioma = sql"INSERT IGNORE INTO Idiomas (iso_639_1, name) VALUES ($origLang, 'Unknown')".update.run

    val insertPelicula = sql"""
      INSERT IGNORE INTO Peliculas (
        idPelicula, imdb_id, title, original_title, overview, tagline,
        adult, video, status, release_date, budget, revenue, runtime,
        popularity, vote_average, vote_count, homepage, poster_path,
        original_language
      ) VALUES (
        $pId,
        ${row.getOrElse("imdb_id", "").trim.take(20)},
        ${row.getOrElse("title", "").trim.take(150)},
        ${row.getOrElse("original_title", "").trim.take(150)},
        ${row.getOrElse("overview", "").trim.take(2000)},
        ${row.getOrElse("tagline", "").trim.take(255)},
        ${if(row.getOrElse("adult", "false").contains("true")) 1 else 0},
        ${if(row.getOrElse("video", "false").contains("true")) 1 else 0},
        ${row.getOrElse("status", "Released").trim.take(20)},
        ${val d = row.getOrElse("release_date", "").trim; if(d.matches("\\d{4}-\\d{2}-\\d{2}")) d else "1900-01-01"},
        ${safeDouble(row.getOrElse("budget", "0"))},
        ${safeDouble(row.getOrElse("revenue", "0"))},
        ${safeInt(row.getOrElse("runtime", "0"))},
        ${safeDouble(row.getOrElse("popularity", "0"))},
        ${safeDouble(row.getOrElse("vote_average", "0"))},
        ${safeInt(row.getOrElse("vote_count", "0"))},
        ${row.getOrElse("homepage", "").trim.take(255)},
        ${row.getOrElse("poster_path", "").trim.take(255)},
        $origLang
      )
    """.update.run

    val genres = parseJsonField[Genres](row.getOrElse("genres", "[]"))
    val keywords = parseJsonField[Keywords](row.getOrElse("keywords", "[]"))
    val countries = parseJsonField[Production_Countries](row.getOrElse("production_countries", "[]"))
    val companies = parseJsonField[Production_Companies](row.getOrElse("production_companies", "[]"))
    val spokenLangs = parseJsonField[Spoken_Languages](row.getOrElse("spoken_languages", "[]"))
    val collectionOpt = parseJsonFieldSingle[BelongToCollection](row.getOrElse("belongs_to_collection", "{}"))
    val cast = parseJsonField[Cast](row.getOrElse("cast", "[]"))
    val crew = parseJsonField[Crew](row.getOrElse("crew", "[]"))
    val ratings = parseJsonField[Ratings](row.getOrElse("ratings", "[]"))

    val insertGeneros = genres.traverse { g =>
      sql"INSERT IGNORE INTO Generos (idGenero, name) VALUES (${g.id}, ${g.name.take(50)})".update.run *>
        sql"INSERT IGNORE INTO Peliculas_Generos (idPelicula, idGenero) VALUES ($pId, ${g.id})".update.run
    }

    val insertKeywords = keywords.traverse { k =>
      sql"INSERT IGNORE INTO Keywords (idKeyword, name) VALUES (${k.id}, ${k.name.take(100)})".update.run *>
        sql"INSERT IGNORE INTO Peliculas_Keywords (idPelicula, idKeyword) VALUES ($pId, ${k.id})".update.run
    }

    val insertCountries = countries.traverse { c =>
      sql"INSERT IGNORE INTO Paises (iso_3166_1, name) VALUES (${c.iso_3166_1.take(2)}, ${c.name.take(100)})".update.run *>
        sql"INSERT IGNORE INTO Peliculas_Paises (idPelicula, iso_3166_1) VALUES ($pId, ${c.iso_3166_1.take(2)})".update.run
    }

    val insertCompanies = companies.traverse { c =>
      sql"INSERT IGNORE INTO Companias (idCompania, name, origin_country) VALUES (${c.id}, ${c.name.take(100)}, ${c.origin_country.map(_.take(2))})".update.run *>
        sql"INSERT IGNORE INTO Peliculas_Companias (idPelicula, idCompania) VALUES ($pId, ${c.id})".update.run
    }

    val insertSpoken = spokenLangs.traverse { l =>
      sql"INSERT INTO Idiomas (iso_639_1, name) VALUES (${l.iso_639_1.take(2)}, ${l.name.take(50)}) ON DUPLICATE KEY UPDATE name = VALUES(name)".update.run *>
        sql"INSERT IGNORE INTO Peliculas_Idiomas (idPelicula, iso_639_1) VALUES ($pId, ${l.iso_639_1.take(2)})".update.run
    }

    val insertCollection = collectionOpt.map { c =>
      sql"INSERT IGNORE INTO Colecciones (idColeccion, name, poster_path, backdrop_path) VALUES (${c.id}, ${c.name.take(100)}, ${c.poster_path.map(_.take(255))}, ${c.backdrop_path.map(_.take(255))})".update.run *>
        sql"INSERT IGNORE INTO Peliculas_Colecciones (idPelicula, idColeccion) VALUES ($pId, ${c.id})".update.run
    }.getOrElse(FC.unit)

    val insertCast = cast.traverse { a =>
      sql"INSERT IGNORE INTO Actores (idActor, name, gender, profile_path) VALUES (${a.cast_id}, ${a.name.take(100)}, ${a.gender.getOrElse(0)}, ${a.profile_path.map(_.take(255))})".update.run *>
        sql"INSERT IGNORE INTO Asignaciones (idActor, idPelicula, `character`, cast_order, credit_id) VALUES (${a.cast_id}, $pId, ${a.character.map(_.take(150))}, ${a.order.getOrElse(0)}, ${a.credit_id.map(_.take(50))})".update.run
    }

    val insertCrew = crew.traverse { c =>
      for {
        _ <- sql"INSERT IGNORE INTO Departamentos (name) VALUES (${c.department.take(50)})".update.run
        _ <- sql"INSERT IGNORE INTO Personal (idPersonal, name, gender, profile_path) VALUES (${c.id}, ${c.name.take(100)}, ${c.gender.getOrElse(0)}, ${c.profile_path.map(_.take(255))})".update.run
        // Subconsulta para obtener ID del departamento recién insertado/existente
        _ <- sql"""
          INSERT IGNORE INTO Trabajos (idPersonal, idPelicula, idDepartamento, job, credit_id)
          VALUES (
            ${c.id}, $pId,
            (SELECT idDepartamento FROM Departamentos WHERE name = ${c.department.take(50)} LIMIT 1),
            ${c.job.take(100)}, ${c.credit_id.map(_.take(50))}
          )
        """.update.run
      } yield ()
    }

    val insertRatings = ratings.traverse { r =>
      sql"INSERT IGNORE INTO Usuarios (userId) VALUES (${r.userId})".update.run *>
        sql"INSERT IGNORE INTO Calificaciones (userId, idPelicula, rating, timestamp) VALUES (${r.userId}, $pId, ${r.rating}, ${r.timestamp})".update.run
    }

    for {
      _ <- insertIdioma
      _ <- insertPelicula
      _ <- insertGeneros
      _ <- insertKeywords
      _ <- insertCountries
      _ <- insertCompanies
      _ <- insertSpoken
      _ <- insertCollection
      _ <- insertCast
      _ <- insertCrew
      _ <- insertRatings
    } yield ()
  }

  def logProgress(index: Long, interval: Int): IO[Unit] =
    if (index % interval == 0) IO.println(s" Procesadas $index filas") else IO.unit

  def logError(index: Long, e: Throwable): IO[Unit] =
    IO.println(s" ERROR Fila $index: ${e.getMessage}")
}