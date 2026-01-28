error id: file:///C:/Users/Lenin/Desktop/Practicum-1.1/Practicum1.1/src/main/scala/utilities/Estadistico.scala:
file:///C:/Users/Lenin/Desktop/Practicum-1.1/Practicum1.1/src/main/scala/utilities/Estadistico.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -scala/math/NumEstadisticas#
	 -NumEstadisticas#
	 -scala/Predef.NumEstadisticas#
offset: 234
uri: file:///C:/Users/Lenin/Desktop/Practicum-1.1/Practicum1.1/src/main/scala/utilities/Estadistico.scala
text:
```scala
package utilities

import models.NUmEstadisticas
import scala.math._

object Estadistico {

  /**
   * Calcula estadísticas descriptivas completas para una lista de valores
   */
  def calculateStats(values: List[Double]): N@@umEstadisticas = {
    if (values.isEmpty) {
      return NumEstadisticas(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }

    val sorted = values.sorted
    val n = values.length
    val mean = values.sum / n.toDouble

    // Mediana
    val median = if (n % 2 == 0) {
      (sorted(n / 2 - 1) + sorted(n / 2)) / 2.0
    } else {
      sorted(n / 2)
    }

    // Desviación estándar
    val variance = values.map(v => pow(v - mean, 2)).sum / n
    val stdDev = sqrt(variance)

    // Cuartiles
    val q1 = sorted((n * 0.25).toInt)
    val q3 = sorted((n * 0.75).toInt)
    val iqr = q3 - q1

    NumEstadisticas(
      count = n,
      mean = mean,
      median = median,
      stdDev = stdDev,
      min = sorted.head,
      max = sorted.last,
      q1 = q1,
      q3 = q3,
      iqr = iqr
    )
  }

  /**
   * Calcula frecuencias para datos categóricos
   */
  def calculateFrequency[T](values: List[T]): Map[T, Int] = {
    values.groupBy(identity).view.mapValues(_.length).toMap
  }

  /**
   * Obtiene el top N más frecuentes
   */
  def topN[T](frequencies: Map[T, Int], n: Int): List[(T, Int)] = {
    frequencies.toList.sortBy(-_._2).take(n)
  }

  /**
   * Detecta outliers usando el método IQR
   */
  def detectOutliers(values: List[Double], multiplier: Double = 1.5): List[Double] = {
    val stats = calculateStats(values)
    val lowerBound = stats.q1 - (multiplier * stats.iqr)
    val upperBound = stats.q3 + (multiplier * stats.iqr)

    values.filter(v => v < lowerBound || v > upperBound)
  }

  /**
   * Calcula correlación de Pearson entre dos listas
   */
  def pearsonCorrelation(x: List[Double], y: List[Double]): Double = {
    require(x.length == y.length && x.nonEmpty, "Las listas deben tener la misma longitud y no estar vacías")

    val n = x.length
    val meanX = x.sum / n
    val meanY = y.sum / n

    val numerator = x.zip(y).map { case (xi, yi) =>
      (xi - meanX) * (yi - meanY)
    }.sum

    val denomX = sqrt(x.map(xi => pow(xi - meanX, 2)).sum)
    val denomY = sqrt(y.map(yi => pow(yi - meanY, 2)).sum)

    if (denomX == 0.0 || denomY == 0.0) 0.0
    else numerator / (denomX * denomY)
  }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 