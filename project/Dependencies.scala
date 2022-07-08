import sbt._

object Dependencies {

  object Version {
    val akka = "2.6.19"
    val akkaHttp = "10.2.9"
    val scalaTest = "3.2.12"
  }

  val akkaStreams = "com.typesafe.akka" %% "akka-stream" % Version.akka
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % Version.akkaHttp
  val akkaHttpSpray = "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.9"
  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest % "test, it"
  val akkaHttpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % Version.akkaHttp % "test,it"

  lazy val akkaDependencies: Seq[ModuleID] = Seq(akkaStreams, akkaHttp, akkaHttpSpray)
  lazy val coreTestDependencies: Seq[ModuleID] = Seq(scalaTest)
  lazy val allTestDependencies: Seq[sbt.ModuleID] = coreTestDependencies ++ Seq(akkaHttpTestKit)
}
