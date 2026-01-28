package utilities

import io.circe.parser.decode
import io.circe.Decoder
import io.circe.generic.auto._
import cats.syntax.either._

object Parsear_JSON {

  /**
   * Limpia strings con formato Python (comillas simples, None, nan)
   * para convertirlos en JSON estándar válido.
   */
  def cleanJsonString(raw: String): String = {
    if (raw == null) return "[]"
    val trimmed: String = raw.trim
    if (trimmed.isEmpty || trimmed == "null" || trimmed.equalsIgnoreCase("nan")) return "[]"

    trimmed
      .replace("'", "\"")       
      .replace("None", "null") 
      .replace("True", "true") 
      .replace("False", "false")
      .replace("nan", "null")
      .replace("\"{", "{").replace("}\"", "}")
  }

  /**
   * Parsea una lista de objetos JSON.
   */
  def parseJsonField[T](rawJson: String)(implicit decoder: Decoder[List[T]]): List[T] = {
    val jsonLimpio: String = cleanJsonString(rawJson)
    if (jsonLimpio == "[]") return List.empty

    decode[List[T]](jsonLimpio).getOrElse(List.empty)
  }

  /**
   * Parsea un OBJETO JSON único (para belongs_to_collection).
   */
  def parseJsonFieldSingle[T](rawJson: String)(implicit decoder: Decoder[T]): Option[T] = {
    val jsonLimpio: String = cleanJsonString(rawJson)
    if (jsonLimpio == "[]" || jsonLimpio == "{}") return None

    // Si es una colección vacía de Python "{}" o string vacío
    if (jsonLimpio == "{}") return None

    decode[T](jsonLimpio).toOption
  }

  /**
   * Versión segura que devuelve Either.
   */
  def parseJsonFieldSafe[T](rawJson: String)(implicit decoder: Decoder[List[T]]): Either[String, List[T]] = {
    val jsonLimpio: String = cleanJsonString(rawJson)
    if (jsonLimpio == "[]") return Right(List.empty)

    decode[List[T]](jsonLimpio).leftMap(_.getMessage)
  }
}