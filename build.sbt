val scala3Version = "3.7.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "Tp3",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.0.0" % Test,
      "org.gnieh" %% "fs2-data-csv" % "1.10.0",
      "co.fs2" %% "fs2-io" % "3.9.3",
      "com.github.haifengl" % "smile-core" % "3.0.0",
      "org.typelevel" %% "cats-core" % "2.10.0",
      "org.bytedeco" % "openblas" % "0.3.21-1.5.8" classifier "macosx-arm64",
      "org.bytedeco" % "openblas-platform" % "0.3.21-1.5.8"    
    )
  )
