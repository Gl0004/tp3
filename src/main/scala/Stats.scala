package tp3

object Stats {

  def mean(values: List[Double]): Double =
    if (values.isEmpty) 0.0 else values.sum / values.size

  def max(values: List[Double]): Double =
    if (values.isEmpty) 0.0 else values.max

  def min(values: List[Double]): Double =
    if (values.isEmpty) 0.0 else values.min
    
  def standardDeviation(values: List[Double]): Double = {
  if (values.isEmpty) 0.0
  else {
    val avg = mean(values)
    val squaredDiffs = values.map(v => math.pow(v - avg, 2))
    math.sqrt(squaredDiffs.sum / values.size)
    }
  }

  def median(values: List[Double]): Double = {
    if (values.isEmpty) 0.0
    else {
      val sorted = values.sorted
      val n = values.size
      if (n % 2 == 1) sorted(n / 2)
      else (sorted((n - 1) / 2) + sorted(n / 2)) / 2.0
    }
  }

  case class StatsResult(
    mean: Double, 
    median: Double, 
    stdDev: Double,
    min: Double,
    max: Double
  )

  def computeStats(values: List[Double]): StatsResult = {
    StatsResult(
      mean = mean(values),
      median = median(values),
      stdDev = standardDeviation(values),
      min = min(values),
      max = max(values)
    )
  }
}