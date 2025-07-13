package tp3

import tp3.Stats.StatsResult
import cats.effect.IO
import fs2.Stream
import fs2.io.file.{Files, Path}
import fs2.text
import tp3.ML.TrainingResult

object Export {
  def writeAggregationToCsv(
    outputPath: Path, 
    data: Map[(String, String, String, Int), Double]
  ): IO[Unit] = {
    val header = "code_postal,commune,type_dechet,nombre_personnes_foyer,total_kg_par_type"

    val rows = data.map { case ((cp, com, td, nbf), total) =>
      s"$cp,$com,$td,$nbf,$total"
    }.toList

    val content = (header :: rows).mkString("\n")

    Stream.emit(content)
      .through(text.utf8.encode)
      .through(Files[IO].writeAll(outputPath))
      .compile
      .drain
  }

  def writeStatsToCsv(
    outputPath: Path, 
    stats: StatsResult
  ): IO[Unit] = {
    val header = "mean,median,std_dev,min,max"
    val row = f"${stats.mean}%.2f,${stats.median}%.2f,${stats.stdDev}%.2f,${stats.min}%.2f,${stats.max}%.2f"
    
    val content = header + "\n" + row
    
    Stream.emit(content)
      .through(text.utf8.encode)
      .through(Files[IO].writeAll(outputPath))
      .compile
      .drain
  }

  def writeModelResultToCsv(
    outputPath: Path, 
    result: TrainingResult
  ): IO[Unit] = {
    val header = "metric,value"
    
    val coefficientRows = result.coefficients.map { case (name, value) =>
      s"$name,$value"
    }
    
    val metricRows = List(
      s"R-squared,${result.rSquared}",
      s"Adjusted R-squared,${result.adjustedRSquared}"
    )
    
    val allRows = header :: (coefficientRows ++ metricRows)
    val content = allRows.mkString("\n")
    
    Stream.emit(content)
      .through(text.utf8.encode)
      .through(Files[IO].writeAll(outputPath))
      .compile
      .drain
  }
}