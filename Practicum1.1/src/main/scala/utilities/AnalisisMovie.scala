package utilities
import cats.effect.IO
import cats.implicits._
import models._
import utilities._

object AnalisisMovie {

  /**
   * Análisis completo de estadísticas numéricas de películas
   * VERSIÓN MEJORADA: Muestra estadísticas con y sin ceros
   */
  def analyzeMovieStats(movies: List[Movie]): IO[Unit] = {
    if (movies.isEmpty) {
      IO.println("ERROR: No hay datos para analizar")
    } else {
      val n = movies.length

      // Calcular estadísticas INCLUYENDO ceros
      val revenueStats = Estadistico.calculateStats(movies.map(_.revenue))
      val budgetStats = Estadistico.calculateStats(movies.map(_.budget))
      val runtimeStats = Estadistico.calculateStats(movies.map(_.runtime).filter(_ > 0))
      val voteStats = Estadistico.calculateStats(movies.map(_.vote_average).filter(_ > 0))

      // 🔥 NUEVO: Calcular estadísticas SIN ceros
      val revenueNonZero = movies.map(_.revenue).filter(_ > 0)
      val budgetNonZero = movies.map(_.budget).filter(_ > 0)

      val revenueStatsNonZero = Estadistico.calculateStats(revenueNonZero)
      val budgetStatsNonZero = Estadistico.calculateStats(budgetNonZero)

      // Contar películas con valores en 0
      val revenueZeros = movies.count(_.revenue == 0.0)
      val budgetZeros = movies.count(_.budget == 0.0)

      // Análisis de idiomas
      val languageFreq = Estadistico.calculateFrequency(movies.map(_.original_language))
      val topLanguages = Estadistico.topN(languageFreq, 5)

      // Imprimir resultados
      IO.println("=" * 70) >>
        IO.println("    ANÁLISIS ESTADÍSTICO DE PELÍCULAS") >>
        IO.println("=" * 70) >>
        IO.println(s"\nTotal de películas analizadas: $n") >>
      // INGRESOS (Revenue)
        IO.println("\n[1] ESTADÍSTICAS DE INGRESOS (Revenue)") >>
        IO.println("-" * 70) >>
        IO.println(f"⚠️  Películas con revenue = 0: $revenueZeros%,d (${revenueZeros*100.0/n}%.1f%%)") >>
        IO.println(f"✅  Películas con revenue > 0: ${revenueNonZero.length}%,d (${revenueNonZero.length*100.0/n}%.1f%%)") >>
        IO.println("\n📊 TODOS LOS DATOS (incluyendo ceros):") >>
        printNumericStats(revenueStats) >>
        (if (revenueNonZero.nonEmpty) {
          IO.println(f"\n💰 SOLO PELÍCULAS CON INGRESOS REPORTADOS (${revenueNonZero.length}%,d películas):") >>
            printNumericStats(revenueStatsNonZero)
        } else IO.unit) >>
      // PRESUPUESTO (Budget)
        IO.println("\n[2] ESTADÍSTICAS DE PRESUPUESTO (Budget)") >>
        IO.println("-" * 70) >>
        IO.println(f"⚠️  Películas con budget = 0: $budgetZeros%,d (${budgetZeros*100.0/n}%.1f%%)") >>
        IO.println(f"✅  Películas con budget > 0: ${budgetNonZero.length}%,d (${budgetNonZero.length*100.0/n}%.1f%%)") >>
        IO.println("\n📊 TODOS LOS DATOS (incluyendo ceros):") >>
        printNumericStats(budgetStats) >>
        (if (budgetNonZero.nonEmpty) {
          IO.println(f"\n💵 SOLO PELÍCULAS CON PRESUPUESTO REPORTADO (${budgetNonZero.length}%,d películas):") >>
            printNumericStats(budgetStatsNonZero)
        } else IO.unit) >>
      // DURACIÓN (Runtime)
      IO.println("\n[3] ESTADÍSTICAS DE DURACIÓN (Runtime)") >>
        IO.println("-" * 70) >>
        printNumericStats(runtimeStats) >>
      // CALIFICACIÓN (Vote Average)
      IO.println("\n[4] ESTADÍSTICAS DE CALIFICACIÓN (Vote Average)") >>
        IO.println("-" * 70) >>
        printNumericStats(voteStats) >>
      // IDIOMAS
      IO.println("\n[5] TOP 5 IDIOMAS MÁS FRECUENTES") >>
        IO.println("-" * 70) >>
        topLanguages.traverse { case (lang, count) =>
          val percentage = (count.toDouble / n * 100)
          IO.println(f"  $lang%-10s: $count%5d películas ($percentage%5.2f%%)")
        }.void >>
        IO.println("=" * 70)
    }
  }

  /**
   * Análisis bivariable: correlaciones entre variables
   * VERSIÓN MEJORADA: Muestra cuántos datos se usan
   */
  def analyzeBivariable(movies: List[Movie]): IO[Unit] = {
    if (movies.isEmpty) {
      IO.println("ERROR: No hay datos para analizar")
    } else {
      // Filtrar películas con datos completos
      val validMovies = movies.filter(m =>
        m.budget > 0 && m.revenue > 0 && m.vote_average > 0
      )

      val totalMovies = movies.length
      val validCount = validMovies.length
      val invalidCount = totalMovies - validCount

      if (validMovies.length < 2) {
        IO.println("ERROR: No hay suficientes datos válidos para análisis bivariable")
      } else {
        val budgets = validMovies.map(_.budget)
        val revenues = validMovies.map(_.revenue)
        val votes = validMovies.map(_.vote_average)
        val popularity = validMovies.map(_.popularity)

        // Calcular correlaciones
        val corrBudgetRevenue = Estadistico.pearsonCorrelation(budgets, revenues)
        val corrBudgetVote = Estadistico.pearsonCorrelation(budgets, votes)
        val corrRevenueVote = Estadistico.pearsonCorrelation(revenues, votes)
        val corrVotePopularity = Estadistico.pearsonCorrelation(votes, popularity)

        IO.println("\n" + "=" * 70) >>
          IO.println("ANÁLISIS BIVARIABLE - CORRELACIONES") >>
          IO.println("=" * 70) >>
          IO.println(s"\nPelículas totales:     $totalMovies") >>
          IO.println(f"Películas analizadas:  $validCount%,d (${validCount*100.0/totalMovies}%.1f%%)") >>
          IO.println(f"Películas excluidas:   $invalidCount%,d (${invalidCount*100.0/totalMovies}%.1f%%)") >>
          IO.println(f"(excluidas por tener budget=0, revenue=0 o vote=0)") >>
          IO.println("\nCOEFICIENTES DE CORRELACIÓN DE PEARSON:") >>
          IO.println("-" * 70) >>
          IO.println(f"Budget vs Revenue:      $corrBudgetRevenue%7.4f  ${interpretCorrelation(corrBudgetRevenue)}") >>
          IO.println(f"Budget vs Vote Avg:     $corrBudgetVote%7.4f  ${interpretCorrelation(corrBudgetVote)}") >>
          IO.println(f"Revenue vs Vote Avg:    $corrRevenueVote%7.4f  ${interpretCorrelation(corrRevenueVote)}") >>
          IO.println(f"Vote Avg vs Popularity: $corrVotePopularity%7.4f  ${interpretCorrelation(corrVotePopularity)}") >>
          IO.println("\nINTERPRETACIÓN:") >>
          IO.println("* Correlación cercana a +1 indica relación positiva fuerte") >>
          IO.println("* Correlación cercana a -1 indica relación negativa fuerte") >>
          IO.println("* Correlación cercana a  0 indica poca o ninguna relación") >>
          IO.println("=" * 70)
      }
    }
  }


  /**
   * Helper: Interpreta el valor de correlación
   */
  def interpretCorrelation(r: Double): String = {
    val absR = math.abs(r)
    val strength = if (absR >= 0.7) "Fuerte"
    else if (absR >= 0.4) "Moderada"
    else if (absR >= 0.2) "Débil"
    else "Muy débil/Ninguna"

    val direction = if (r > 0) "positiva" else if (r < 0) "negativa" else "sin dirección"
    s"($strength $direction)"
  }

  /**
   * Helper: Imprime estadísticas numéricas formateadas
   */
  def printNumericStats(stats: NumEstadistica): IO[Unit] = {
    IO.println(f"  Conteo:                ${stats.count}%,d") >>
      IO.println(f"  Promedio (Media):      $$${stats.mean}%,.2f") >>
      IO.println(f"  Mediana:               $$${stats.median}%,.2f") >>
      IO.println(f"  Desviación Estándar:   $$${stats.stdDev}%,.2f") >>
      IO.println(f"  Mínimo:                $$${stats.min}%,.2f") >>
      IO.println(f"  Máximo:                $$${stats.max}%,.2f") >>
      IO.println(f"  Q1 (25%%):              $$${stats.q1}%,.2f") >>
      IO.println(f"  Q3 (75%%):              $$${stats.q3}%,.2f") >>
      IO.println(f"  IQR (Rango IntQ):      $$${stats.iqr}%,.2f")
  }
}