import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.8"
ThisBuild / organization := "com.twosumarmy"

lazy val root = (project in file("."))
  .settings(
    name := "bank-transaction-service",
    libraryDependencies ++= akkaDependencies ++ allTestDependencies
  )
