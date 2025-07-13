package tp3

import smile.regression.OLS
import smile.regression.LinearModel
import smile.data.DataFrame
import smile.data.formula.Formula
import smile.data.vector.DoubleVector
import cats.data.{NonEmptyList, ValidatedNel}
import cats.syntax.all._
import scala.jdk.CollectionConverters._

object ML {

  case class AggRecord(
    postalCode: Double,
    wasteType: Int,
    householdSize: Int,
    wasteKg: Double
  )

  case class TrainingResult(
    model: LinearModel,
    coefficients: List[(String, Double)],
    rSquared: Double,
    adjustedRSquared: Double
  )

  sealed trait TrainingError
  case object EmptyDataset extends TrainingError
  case class ConversionError(message: String) extends TrainingError

  def prepareData(
    data: Map[(String, String, String, Int), Double],
    wasteTypeMapping: Map[String, Int]
  ): ValidatedNel[TrainingError, NonEmptyList[AggRecord]] = {
    if (data.isEmpty) EmptyDataset.invalidNel
    else {
      data.toList.traverse { 
        case ((postalCode, _, wasteType, householdSize), wasteKg) =>
          val pcValue = postalCode.toDoubleOption
            .toValidNel(ConversionError(s"invalid postal code: $postalCode"))
          val wtValue = wasteTypeMapping.get(wasteType)
            .toValidNel(ConversionError(s"unknown waste type: $wasteType"))
          (pcValue, wtValue).mapN(AggRecord(_, _, householdSize, wasteKg))
      }.andThen { list =>
        NonEmptyList.fromList(list).toValid(EmptyDataset).toValidatedNel
      }
    }
  }

  def trainModel(data: NonEmptyList[AggRecord]): TrainingResult = {
    // Convert records to list
    val records = data.toList

    // Determine distinct waste types sorted for one-hot encoding
    val wasteTypes = records.map(_.wasteType).distinct.sorted
    val wasteTypeToIndex = wasteTypes.zipWithIndex.toMap

    // Build feature matrix X and target vector y
    val X: Array[Array[Double]] = records.map { r =>
      // One-hot encode wasteType
      val oneHot = Array.fill[Double](wasteTypes.length)(0.0)
      wasteTypeToIndex.get(r.wasteType).foreach { idx =>
        oneHot(idx) = 1.0
      }
      // Combine numeric features and one-hot vector
      Array(r.postalCode, r.householdSize.toDouble) ++ oneHot
    }.toArray

    val y: Array[Double] = records.map(_.wasteKg).toArray

    // Create DataFrame for features and target
    val featureNames: Array[String] = Array("postalCode", "householdSize") ++ wasteTypes.map(i => s"wasteType_$i")
    val dfX: DataFrame = DataFrame.of(X, featureNames: _*)
    val dfY: DoubleVector = DoubleVector.of("wasteKg", y)
    val df: DataFrame = dfX.merge(dfY)
    val formula = Formula.lhs("wasteKg")

    // Train OLS regression model with intercept
    val model = OLS.fit(formula, df)

    // Extract metrics
    val rSquared = model.RSquared()
    val adjustedRSquared = model.adjustedRSquared()

    // Name coefficients: intercept followed by feature names
    val coefNames =
      "Intercept" ::
      ("postalCode" :: "householdSize" :: wasteTypes.map(i => s"wasteType_$i").toList)

    val coefficients = coefNames.zip(model.coefficients())

    TrainingResult(model, coefficients, rSquared, adjustedRSquared)
  }

  def createWasteTypeMapping(
    data: Map[(String, String, String, Int), Double]
  ): Map[String, Int] = {
    data.keys
      .map { case (_, _, wt, _) => wt }
      .toList.distinct.zipWithIndex.toMap
  }

  def trainLinearModel(
    data: Map[(String, String, String, Int), Double]
  ): ValidatedNel[TrainingError, TrainingResult] = {
    val mapping = createWasteTypeMapping(data)
    prepareData(data, mapping).map(trainModel)
  }
}
