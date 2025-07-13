object Aggregation:

  import Main.Dechet

  /**
   * Group by (codePostal, commune, typeDechet, nombrePersonnesFoyer),
   * and compute the total quantity (kg) per group.
   * Pure functional aggregation using groupMapReduce.
   */
  def totalKgByType(dechets: List[Dechet]): Map[
    (String, String, String, Int), Double
  ] =
    dechets.groupMapReduce { d =>
      (d.codePostal, d.commune, d.typeDechet, d.nombrePersonnesFoyer)
    }(_.quantiteKgParAn)(_ + _)