import scala.collection.Seq

val CirceVersion = "0.14.6"
val Http4sVersion = "1.0.0-M37"
val LogbackVersion = "1.5.18"
val MunitVersion = "0.7.29"
val MunitCatsEffectVersion = "1.0.7"
val Scalatest = "3.2.19"

name := "scalatest-screenshot-investigation-web-app-example"

version := "0.1"

scalaVersion := "3.6.4"

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.http4s" %% "http4s-twirl" % "1.0.0-M38",
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "ch.qos.logback" % "logback-core" % LogbackVersion,
      "org.slf4j" % "jcl-over-slf4j" % "2.0.17",
      "org.slf4j" % "slf4j-api" % "2.0.17",
      "org.typelevel" %% "cats-effect" % "3.6.1",
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-literal" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "commons-io" % "commons-io" % "2.19.0",
      "org.scalameta" %% "munit" % MunitVersion % Test,
      "org.typelevel" %% "munit-cats-effect-3" % MunitCatsEffectVersion % Test,
      "org.scalatestplus" %% "selenium-4-21" % "3.2.19.0" % Test,
      "org.scalatest" %% "scalatest-core" % Scalatest,
      "org.scalatest" %% "scalatest-flatspec" % Scalatest % Test,
      "org.scalatest" %% "scalatest-funsuite" % Scalatest % Test,
      "org.scalatest" %% "scalatest-shouldmatchers" % Scalatest % Test,
      "org.scalatest" %% "scalatest-featurespec" % Scalatest % Test,
      "co.fs2" %% "fs2-io" % "3.11.0" % Test
    ),
    scalacOptions ++= Seq("-Wunused:imports,privates,locals")
  )
  .enablePlugins(JavaAppPackaging, SbtTwirl)

Compile / run / fork := true
