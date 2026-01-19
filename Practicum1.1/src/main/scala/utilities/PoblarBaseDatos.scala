package utilities

import cats.effect.IO
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.free.{connection => FC}
import io.circe.parser._
import io.circe.generic.auto._

object PoblarBaseDatos {

  case class GenreJson(id: Int, name: String)
  case class CollectionJson(id: Int, name: String, poster_path: Option[String], backdrop_path: Option[String])
  case class CompanyJson(id: Int, name: String, origin_country: Option[String])
  case class CountryJson(iso_3166_1: String, name: String)
  case class LanguageJson(iso_639_1: String, name: String)
  case class KeywordJson(id: Int, name: String)
  case class ActorJson(
                        id: Int,
                        name: String,
                        gender: Option[Int],
                        profile_path: Option[String],
                        character: Option[String],
                        order: Option[Int],
                        credit_id: Option[String]
                      )
  case class CrewJson(
                       id: Int,
                       name: String,
                       gender: Option[Int],
                       profile_path: Option[String],
                       department: String,
                       job: String,
                       credit_id: Option[String]
                     )
  case class RatingJson(userId: Int, rating: Double, timestamp: Long)

  def fixJsonFormat(s: String): String = {
    if (s == null || s.trim.isEmpty || s == "null") "[]"
    else s.trim
      .replace("'", "\"")
      .replace("None", "null")
      .replace("True", "true")
      .replace("False", "false")
      .replace("\"{", "{").replace("}\"", "}")
  }

  /**
   * Convierte string a Int de forma segura
   * Maneja decimales (ej: "123.0" -> 123)
   */
  def safeInt(s: String): Int =
    s.trim.toDoubleOption.map(_.toInt).getOrElse(0)

  /**
   * Convierte string a Double de forma segura
   */
  def safeDouble(s: String): Double =
    s.trim.toDoubleOption.getOrElse(0.0)

  /**
   * Decodificador genérico que limpia antes de parsear
   * Convierte JSON Python-style a JSON válido y lo parsea
   */
  def decodeClean[T: io.circe.Decoder](raw: String): List[T] =
    decode[List[T]](fixJsonFormat(raw)).getOrElse(Nil)
  /**
   * Puebla TODAS las tablas relacionadas con una película
   *
   * Orden de inserción:
   * 1. Película principal (con idioma original)
   * 2. Géneros
   * 3. Keywords
   * 4. Países
   * 5. Compañías
   * 6. Idiomas hablados
   * 7. Colecciones
   * 8. Cast (Actores y Asignaciones)
   * 9. Crew (Personal, Departamentos y Trabajos)
   * 10. Ratings (Usuarios y Calificaciones)
   */
  def populateAll(row: Map[String, String]): ConnectionIO[Unit] = {
    val pId = safeInt(row.getOrElse("id", "0"))
    val origLang = row.getOrElse("original_language", "un").trim.take(2)

    // Si el ID es inválido, no hacemos nada
    if (pId == 0) FC.unit
    else for {
      _ <- sql"""
        INSERT IGNORE INTO Idiomas (iso_639_1, name)
        VALUES ($origLang, 'Unknown')
      """.update.run

      // Insertar la película principal
      _ <- sql"""
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
          ${val d = row.getOrElse("release_date", "").trim
        if(d.matches("\\d{4}-\\d{2}-\\d{2}")) d else "1900-01-01"},
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

      // GÉNEROS
      _ <- decodeClean[GenreJson](row.getOrElse("genres", "[]")).traverse { g =>
        sql"""
          INSERT IGNORE INTO Generos (idGenero, name)
          VALUES (${g.id}, ${g.name.take(50)})
        """.update.run *>
          sql"""
          INSERT IGNORE INTO Peliculas_Generos (idPelicula, idGenero)
          VALUES ($pId, ${g.id})
        """.update.run
      }

      // KEYWORDS
      _ <- decodeClean[KeywordJson](row.getOrElse("keywords", "[]")).traverse { k =>
        sql"""
          INSERT IGNORE INTO Keywords (idKeyword, name)
          VALUES (${k.id}, ${k.name.take(100)})
        """.update.run *>
          sql"""
          INSERT IGNORE INTO Peliculas_Keywords (idPelicula, idKeyword)
          VALUES ($pId, ${k.id})
        """.update.run
      }

      // PAÍSES DE PRODUCCIÓN
      _ <- decodeClean[CountryJson](row.getOrElse("production_countries", "[]")).traverse { c =>
        sql"""
          INSERT IGNORE INTO Paises (iso_3166_1, name)
          VALUES (${c.iso_3166_1.take(2)}, ${c.name.take(100)})
        """.update.run *>
          sql"""
          INSERT IGNORE INTO Peliculas_Paises (idPelicula, iso_3166_1)
          VALUES ($pId, ${c.iso_3166_1.take(2)})
        """.update.run
      }

      // COMPAÑÍAS DE PRODUCCIÓN
      _ <- decodeClean[CompanyJson](row.getOrElse("production_companies", "[]")).traverse { c =>
        sql"""
          INSERT IGNORE INTO Companias (idCompania, name, origin_country)
          VALUES (${c.id}, ${c.name.take(100)}, ${c.origin_country.map(_.take(2))})
        """.update.run *>
          sql"""
          INSERT IGNORE INTO Peliculas_Companias (idPelicula, idCompania)
          VALUES ($pId, ${c.id})
        """.update.run
      }

      // IDIOMAS HABLADOS
      _ <- decodeClean[LanguageJson](row.getOrElse("spoken_languages", "[]")).traverse { l =>
        sql"""
          INSERT INTO Idiomas (iso_639_1, name)
          VALUES (${l.iso_639_1.take(2)}, ${l.name.take(50)})
          ON DUPLICATE KEY UPDATE name = VALUES(name)
        """.update.run *>
          sql"""
          INSERT IGNORE INTO Peliculas_Idiomas (idPelicula, iso_639_1)
          VALUES ($pId, ${l.iso_639_1.take(2)})
        """.update.run
      }

      // COLECCIONES

      _ <- {
        val rawColl = row.getOrElse("belongs_to_collection", "")
        val collStr = fixJsonFormat(
          if (rawColl.startsWith("{")) s"[$rawColl]" else rawColl
        )

        decode[List[CollectionJson]](collStr).getOrElse(Nil).traverse { c =>
          sql"""
            INSERT IGNORE INTO Colecciones (idColeccion, name, poster_path, backdrop_path)
            VALUES (${c.id}, ${c.name.take(100)}, ${c.poster_path.map(_.take(255))}, ${c.backdrop_path.map(_.take(255))})
          """.update.run *>
            sql"""
            INSERT IGNORE INTO Peliculas_Colecciones (idPelicula, idColeccion)
            VALUES ($pId, ${c.id})
          """.update.run
        }
      }

      // CAST (Actores y sus personajes)
      _ <- decodeClean[ActorJson](row.getOrElse("cast", "[]")).traverse { a =>
        sql"""
          INSERT IGNORE INTO Actores (idActor, name, gender, profile_path)
          VALUES (${a.id}, ${a.name.take(100)}, ${a.gender.getOrElse(0)}, ${a.profile_path.map(_.take(255))})
        """.update.run *>
          sql"""
          INSERT IGNORE INTO Asignaciones (idActor, idPelicula, `character`, cast_order, credit_id)
          VALUES (${a.id}, $pId, ${a.character.map(_.take(150))}, ${a.order.getOrElse(0)}, ${a.credit_id.map(_.take(50))})
        """.update.run
      }

      // CREW (Personal técnico, departamentos y trabajos)
      _ <- decodeClean[CrewJson](row.getOrElse("crew", "[]")).traverse { c =>
        for {
          // Insertar departamento si no existe
          _ <- sql"""
            INSERT IGNORE INTO Departamentos (name)
            VALUES (${c.department.take(50)})
          """.update.run

          // Insertar persona
          _ <- sql"""
            INSERT IGNORE INTO Personal (idPersonal, name, gender, profile_path)
            VALUES (${c.id}, ${c.name.take(100)}, ${c.gender.getOrElse(0)}, ${c.profile_path.map(_.take(255))})
          """.update.run

          // Insertar trabajo (relacionando persona + película + departamento)
          _ <- sql"""
            INSERT IGNORE INTO Trabajos (idPersonal, idPelicula, idDepartamento, job, credit_id)
            VALUES (
              ${c.id},
              $pId,
              (SELECT idDepartamento FROM Departamentos WHERE name = ${c.department.take(50)} LIMIT 1),
              ${c.job.take(100)},
              ${c.credit_id.map(_.take(50))}
            )
          """.update.run
        } yield ()
      }

      // RATINGS (Usuarios y sus calificaciones)
      _ <- decodeClean[RatingJson](row.getOrElse("ratings", "[]")).traverse { r =>
        sql"""
          INSERT IGNORE INTO Usuarios (userId)
          VALUES (${r.userId})
        """.update.run *>
          sql"""
          INSERT IGNORE INTO Calificaciones (userId, idPelicula, rating, timestamp)
          VALUES (${r.userId}, $pId, ${r.rating}, ${r.timestamp})
        """.update.run
      }

    } yield ()
  }

  /**
   * Mensaje de progreso para logging
   * @param index Número de fila procesada
   * @param interval Cada cuántas filas mostrar progreso
   */
  def logProgress(index: Long, interval: Int = 200): IO[Unit] = {
    if (index % interval == 0)
      IO.println(s" Procesadas $index filas")
    else
      IO.unit
  }

  /**
   * Manejo de errores para una fila específica
   * @param index Número de fila
   * @param error Error ocurrido
   */
  def logError(index: Long, error: Throwable): IO[Unit] = {
    IO.println(s" Error en fila $index: ${error.getMessage.take(100)}")
  }

}