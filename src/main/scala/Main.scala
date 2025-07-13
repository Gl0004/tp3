import Aggregation.totalKgByType
import tp3.Export
import tp3.Stats
import tp3.ML
import tp3.ML._
import cats.effect.{IO, IOApp}
import cats.data.Validated.{Invalid, Valid}
import cats.syntax.all._
import fs2._
import fs2.io.file.{Files, Path}
import fs2.text.{utf8, lines}
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Main extends IOApp.Simple {

  val csvPath = Path("src/main/resources/donnees_dechets_paris.csv")

  // Data structure representing a waste record
  case class Dechet(
    id: String,
    adresse: String,
    codePostal: String,
    commune: String,
    nombrePersonnesFoyer: Int,
    typeDechet: String,
    quantiteKgParAn: Double,
    modeCollecte: String,
    triEffectue: Boolean,
    dateCollecte: LocalDate,
    surfaceLogement: Double,
    revenuMenage: Double,
    ageMoyen: Int,
    nbDechetsParMois: Double,
    densitePopulation: Double
  )

  // Date formatter for "yyyy-MM-dd" format
  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  /**
    * Parse a CSV row into Dechet case class
    * Performs the following cleaning & conversions:
    * 1. Drops rows with insufficient columns (similar to dropna(how="all"))
    * 2. Converts numeric fields using toInt/toDouble
    * 3. Converts triEffectue string to Boolean
    * 4. Parses dateCollecte string to LocalDate
    */
  def parseLine(cols: List[String]): Option[Dechet] = {
    if (cols.length < 15) None
    else {
      try {
        val triBool = cols(8).trim.toLowerCase match {
          case "oui" => true
          case "non" => false
          case _     => false  // Default to false for invalid values
        }
        
        // Parse date string to LocalDate
        val date = LocalDate.parse(cols(9).trim, dateFormatter)

        Some(Dechet(
          id                    = cols(0).trim,
          adresse               = cols(1).trim,
          codePostal            = cols(2).trim,
          commune               = cols(3).trim,
          nombrePersonnesFoyer  = cols(4).trim.toInt,
          typeDechet            = cols(5).trim,
          quantiteKgParAn       = cols(6).trim.toDouble,
          modeCollecte          = cols(7).trim,
          triEffectue           = triBool,
          dateCollecte          = date,
          surfaceLogement       = cols(10).trim.toDouble,
          revenuMenage          = cols(11).trim.toDouble,
          ageMoyen              = cols(12).trim.toInt,
          nbDechetsParMois      = cols(13).trim.toDouble,
          densitePopulation     = cols(14).trim.toDouble
        ))
      } catch {
        case _: Throwable => None  // Silently drop malformed rows
      }
    }
  }

  /** 
    * Split a CSV line on commas while preserving quoted fields
    * Handles empty fields and quoted commas correctly
    */
  def splitCsv(line: String): List[String] =
    line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1)
        .toList
        .map(_.trim.stripPrefix("\"").stripSuffix("\""))

  override def run: IO[Unit] = {
    Files[IO]
      .readAll(csvPath)
      .through(utf8.decode)
      .through(lines)
      .filter(_.nonEmpty)      // Drop empty lines
      .drop(1)                 // Skip header row
      .map(splitCsv)           // Split CSV line into columns
      .map(parseLine)          // Parse columns into Dechet case class
      .collect { case Some(d) => d }  // Keep only successfully parsed records
      .filter(_.triEffectue)   // Filter for records with recycling enabled
      .compile
      .toList
      .flatMap { dechets =>
        if (dechets.isEmpty) {
          IO.raiseError(new Exception("No valid data found after parsing CSV"))
        } else {
          // Calculate aggregated data and statistics
          val aggregated = totalKgByType(dechets)
          val quantiteList = dechets.map(_.quantiteKgParAn)
          val stats = Stats.computeStats(quantiteList)
          
          val aggregationPath = Path("src/main/resources/aggregated_results.csv")
          val statsPath = Path("src/main/resources/statistics.csv")
          val modelPath = Path("src/main/resources/model_results.csv")

          // Train linear regression model (pure functional call)
          val modelResult = ML.trainLinearModel(aggregated)
          
          for {
            // Print statistics summary
            _ <- IO.println("\n======= Waste Processing Statistics =======")
            _ <- IO.println(f"• Average annual waste:  ${stats.mean}%.2f kg")
            _ <- IO.println(f"• Median annual waste:   ${stats.median}%.2f kg")
            _ <- IO.println(f"• Standard deviation:    ${stats.stdDev}%.2f kg")
            _ <- IO.println(f"• Minimum annual waste:  ${stats.min}%.2f kg")
            _ <- IO.println(f"• Maximum annual waste:  ${stats.max}%.2f kg")
            _ <- IO.println("============================================")
            
            // Ensure output directory exists
            _ <- Files[IO].createDirectories(aggregationPath.parent.getOrElse(Path(".")))
            
            // Write aggregated results to CSV
            _ <- Export.writeAggregationToCsv(aggregationPath, aggregated)
            _ <- IO.println(s"\nAggregation results saved to: ${aggregationPath.toNioPath.toAbsolutePath}")
            
            // Write statistics summary to CSV
            _ <- Export.writeStatsToCsv(statsPath, stats)
            _ <- IO.println(s"Statistics saved to: ${statsPath.toNioPath.toAbsolutePath}")
            
            // Handle model training results (pure functional error handling)
            _ <- modelResult.fold(
              // Error handling path
              errors => {
                val errorMessages = errors.toList.map {
                  case EmptyDataset => "Error: The dataset is empty"
                  case ConversionError(msg) => s"Data conversion error: $msg"
                }.mkString("\n")
                
                IO.println("\n=== Model Training Failed ===") >>
                IO.println(errorMessages)
              },
              
              // Success handling path
              result =>
                val modelInfo = List(
                  "\n=== Linear Regression Model ===",
                  f"R-squared: ${result.rSquared}%.4f",
                  f"Adjusted R-squared: ${result.adjustedRSquared}%.4f",
                  "Coefficients:"
                ) ++ result.coefficients.map { case (name, value) =>
                  f"  - $name: $value%.6f"
                }

                for {
                  _ <- Export.writeModelResultToCsv(modelPath, result)
                  _ <- IO.println(s"Model results saved to: ${modelPath.toNioPath.toAbsolutePath}")
                  _ <- IO.println(modelInfo.mkString("\n"))
                  _ <- IO.println("==============================================")
                } yield ()
            )
          } yield ()
        }
      }
  }
}