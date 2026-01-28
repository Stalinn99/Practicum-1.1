package utilities

import models.Movie

object Limpieza {

  /**
   * Valida que un ID sea válido (no vacío y solo dígitos)
   */
  def isValidId(idStr: String): Boolean = {
    idStr.trim.nonEmpty && idStr.trim.forall(_.isDigit)
  }

  /**
   * Filtra películas válidas basándose en criterios personalizables
   */
  def filterValidMovies(movies: List[Movie],
                        minBudget: Double = 0.0,
                        minRevenue: Double = 0.0): List[Movie] = {
    movies.filter { m =>
      m.budget >= minBudget &&
        m.revenue >= minRevenue &&
        m.id > 0
    }
  }

  /**
   * Elimina duplicados basándose en el ID
   */
  def removeDuplicatesById(movies: List[Movie]): List[Movie] = {
    movies.groupBy(_.id).values.map(_.head).toList
  }

  /**
   * Normaliza texto: trim, lowercase, etc.
   */
  def normalizeText(text: String): String = {
    text.trim.toLowerCase.replaceAll("\\s+", " ")
  }

  /**
   * Limpia valores extremos (outliers) basándose en percentiles
   */
  def removeOutliers(values: List[Double], lowerPercentile: Double = 0.01, upperPercentile: Double = 0.99): List[Double] = {
    if (values.isEmpty) return List.empty

    val sorted: List[Double] = values.sorted
    val n: Int = sorted.length
    val lowerIndex: Int = (n * lowerPercentile).toInt
    val upperIndex: Int = (n * upperPercentile).toInt

    sorted.slice(lowerIndex, upperIndex)
  }
}