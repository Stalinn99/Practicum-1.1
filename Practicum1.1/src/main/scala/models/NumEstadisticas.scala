package models

case class NumEstadisticas(
                           count: Long,
                           mean: Double,
                           median: Double,
                           stdDev: Double,
                           min: Double,
                           max: Double,
                           q1: Double,
                           q3: Double,
                           iqr: Double
                         )