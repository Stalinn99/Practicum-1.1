package models

case class NumEstadistica(
                           count: Int,
                           mean: Double,
                           median: Double,
                           stdDev: Double,
                           min: Double,
                           max: Double,
                           q1: Double,
                           q3: Double,
                           iqr: Double
                         )