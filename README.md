# TP3 - Analyse et prédiction des déchets ménagers à Paris

## 📁 Structure du projet

```
tp3/
├── build.sbt
├── src/
│   └── main/
│       ├── scala/
│       │   ├── Aggregation.scala
│       │   ├── Export.scala
│       │   ├── Main.scala
│       │   ├── ML.scala
│       └── resources/
│           ├── donnees_dechets_paris.csv
│           ├── aggregated_results.csv
│           ├── statistics.csv
│           ├── model_results.csv
├── dashboard.ipynb
├── README.md
```

## ⚙️ Technologies utilisées

- **Scala 3** + SBT
- **Smile 3.0.0** pour le traitement de données et le modèle de régression
- **Python 3.12** (Jupyter Notebook) pour la visualisation (Pandas, Seaborn, Matplotlib)

## 🔍 Objectifs

1. **Traitement des données** de déchets ménagers à Paris **Données simulées**
2. **Agrégation** des données par type de déchet, nombre de personnes et code postal
3. **Modélisation** d'une régression linéaire pour prédire la quantité annuelle de déchets
4. **Dashboard** Python avec visualisations pour explorer les tendances

## 📊 Visualisations dans le notebook

- Répartition des déchets par **type**
- Moyenne par **taille de foyer**
- Répartition totale par **code postal**
- **Importance des variables** dans le modèle de régression

> ⚠️ **Données simulées** : Les résultats du modèle ne sont pas représentatifs du réel. Les visualisations sont à but pédagogique.
