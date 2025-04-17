import scala.collection.Seq

val CirceVersion = "0.14.6"
val Http4sVersion = "0.23.26"
val LogbackVersion = "1.5.18"
val MunitVersion = "0.7.29"
val MunitCatsEffectVersion = "1.0.7"
val Scalatest = "3.2.19"

name := "scalatest-screenshot-investigation-web-app-example"

version := "0.1"

scalaVersion := "2.13.16"

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.http4s" %% "http4s-twirl" % "0.23.17",
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "ch.qos.logback" % "logback-core" % LogbackVersion,
      "org.slf4j" % "jcl-over-slf4j" % "2.0.17",
      "org.slf4j" % "slf4j-api" % "2.0.17",
      "org.typelevel" %% "cats-effect" % "3.6.1",
      "org.scalameta" %% "munit" % MunitVersion % "test",
      "org.typelevel" %% "munit-cats-effect-3" % MunitCatsEffectVersion % "test",
      "org.scalatestplus" %% "selenium-4-21" % "3.2.19.0" % "test",
      "org.scalatest" %% "scalatest-flatspec" % Scalatest % "test",
      "org.scalatest" %% "scalatest-shouldmatchers" % Scalatest % "test",
      "org.scalatest" %% "scalatest-featurespec" % Scalatest % "test"
    ),
    scalacOptions ++= Seq("-Ywarn-unused")
  )
  .enablePlugins(JavaAppPackaging, SbtTwirl)


Compile / mainClass := Some("com.recommender.Main")

Compile / run / fork := true
