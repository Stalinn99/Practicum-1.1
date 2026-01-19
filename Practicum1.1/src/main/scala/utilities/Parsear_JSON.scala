package utilities

import io.circe.parser.decode
import io.circe.Decoder
import io.circe.generic.auto._

object Parsear_JSON {

  /**
   * Función genérica para limpiar y parsear JSON embebido en CSV
   * Maneja casos comunes como comillas simples, None, nan, etc.
   */
  def parseJsonField[T](rawJson: String)(implicit decoder: Decoder[List[T]]): List[T] = {
    val limpio: String = rawJson.trim

    // Casos especiales que representan listas vacías
    if (limpio.isEmpty || limpio == "[]" || limpio.toLowerCase == "nan") {
      return List.empty
    }

    // Limpieza del JSON
    val jsonLimpio: String = limpio
      .replace("'", "\"")      // Comillas simples → dobles
      .replace("None", "null")  // None de Python → null de JSON
      .replace("nan", "null")   // nan → null

    // Intentar decodificar
    decode[List[T]](jsonLimpio).getOrElse(List.empty)
  }

  /**
   * Parsea un OBJETO JSON único (no un array)
   * Útil para belongsToCollection que es un objeto, no un array
   */
  def parseJsonFieldSingle[T](rawJson: String)(implicit decoder: Decoder[T]): Option[T] = {
    val limpio: String = rawJson.trim

    // Casos especiales que representan objetos vacíos
    if (limpio.isEmpty || limpio == "{}" || limpio == "null" || limpio.toLowerCase == "nan") {
      return None
    }

    // Limpieza del JSON
    val jsonLimpio: String = limpio
      .replace("'", "\"")      // Comillas simples → dobles
      .replace("None", "null")  // None de Python → null de JSON
      .replace("nan", "null")   // nan → null

    // Intentar decodificar como objeto único
    decode[T](jsonLimpio).toOption
  }

  /**
   * Versión que devuelve Either para manejo explícito de errores
   */
  def parseJsonFieldSafe[T](rawJson: String)(implicit decoder: Decoder[List[T]]): Either[String, List[T]] = {
    val limpio: String = rawJson.trim

    if (limpio.isEmpty || limpio == "[]" || limpio.toLowerCase == "nan") {
      return Right(List.empty)
    }

    val jsonLimpio: String = limpio
      .replace("'", "\"")
      .replace("None", "null")
      .replace("nan", "null")

    decode[List[T]](jsonLimpio).left.map(_.getMessage)
  }
}