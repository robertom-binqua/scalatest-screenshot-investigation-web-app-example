import scala.collection.Seq

val CirceVersion = "0.14.6"
val Http4sVersion = "0.23.26"
val LogbackVersion = "1.5.18"
val MunitVersion = "0.7.29"
val MunitCatsEffectVersion = "1.0.7"
val Scalatest = "3.2.19"

name := "scalatest-featurespecs-screenshots-reporter"

version := "0.1"

scalaVersion := "2.13.16"

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.6.1",
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-literal" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "commons-io" % "commons-io" % "2.19.0",
      "org.jsoup" % "jsoup" % "1.20.1",
      "org.scalatestplus" %% "selenium-4-21" % "3.2.19.0",
      "org.scalatest" %% "scalatest-core" % Scalatest,
      "org.http4s" %% "http4s-ember-server" % Http4sVersion % Test,
      "org.http4s" %% "http4s-dsl" % Http4sVersion % Test,
      "org.http4s" %% "http4s-twirl" % "0.23.17" % Test,
      "ch.qos.logback" % "logback-classic" % LogbackVersion % Test,
      "ch.qos.logback" % "logback-core" % LogbackVersion % Test,
      "org.slf4j" % "jcl-over-slf4j" % "2.0.17" % Test,
      "org.slf4j" % "slf4j-api" % "2.0.17" % Test,
      "co.fs2" %% "fs2-io" % "3.11.0" % Test,
      "org.scalameta" %% "munit" % MunitVersion % Test,
      "org.scalatest" %% "scalatest-flatspec" % Scalatest % Test,
      "org.scalatest" %% "scalatest-shouldmatchers" % Scalatest % Test,
      "org.scalatest" %% "scalatest-featurespec" % Scalatest % Test,
      "org.typelevel" %% "munit-cats-effect" % "2.1.0" % Test
    ),
    scalacOptions ++= Seq("-Ywarn-unused")
  )
  .enablePlugins(JavaAppPackaging, SbtTwirl)
