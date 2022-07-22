import sbt._

object Dependencies {

  object Version {
    val akka                    = "2.6.19"
    val alpakkaKafka            = "3.0.0"
    val akkaHttp                = "10.2.9"
    val akkaHttpJsonSerializers = "1.39.2"
    val circe                   = "0.14.1"
    val scalaTest               = "3.2.12"
    val wireMock                = "2.27.2"
    val logBack                 = "1.3.0-alpha16"
  }

  val akkaStreams        = "com.typesafe.akka"     %% "akka-stream"         % Version.akka
  val akkaStreamsTestKit = "com.typesafe.akka"     %% "akka-stream-testkit" % Version.akka
  val akkaStreamsKafka   = "com.typesafe.akka"     %% "akka-stream-kafka"   % Version.alpakkaKafka
  val akkaHttp           = "com.typesafe.akka"     %% "akka-http"           % Version.akkaHttp
  val akkaHttpTestKit    = "com.typesafe.akka"     %% "akka-http-testkit"   % Version.akkaHttp
  val akkaHttpCirce      = "de.heikoseeberger"     %% "akka-http-circe"     % Version.akkaHttpJsonSerializers
  val circeCore          = "io.circe"              %% "circe-core"          % Version.circe
  val circeGeneric       = "io.circe"              %% "circe-generic"       % Version.circe
  val circeParser        = "io.circe"              %% "circe-parser"        % Version.circe
  val scalaTest          = "org.scalatest"         %% "scalatest"           % Version.scalaTest
  val wireMock           = "com.github.tomakehurst" % "wiremock"            % Version.wireMock
  val logBack            = "ch.qos.logback"         % "logback-classic"     % Version.logBack
  val akkaSlf4j          = "com.typesafe.akka"     %% "akka-slf4j"          % Version.akka

  lazy val akkaDependencies: Seq[ModuleID]     = Seq(akkaStreams, akkaStreamsKafka, akkaHttp, logBack, akkaSlf4j)
  lazy val circeDependencies: Seq[ModuleID]    = Seq(circeCore, circeGeneric, circeParser, akkaHttpCirce)
  lazy val coreTestDependencies: Seq[ModuleID] = Seq(scalaTest)
  lazy val allTestDependencies: Seq[ModuleID]  = coreTestDependencies ++ Seq(
    akkaStreamsTestKit,
    akkaHttpTestKit,
    wireMock,
    logBack
  )
}
