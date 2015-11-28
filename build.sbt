name := """SimpleExchange"""

scalaVersion := "2.11.6"

name := "SimpleExchange"

version := "1.0"

lazy val `simpleexchange` = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= {
  val akkaV = "2.2.5"
  val sprayV = "1.3.3"
  Seq(
    jdbc, anorm, cache, ws,
    "io.spray" %% "spray-caching" % sprayV,
    "io.spray" %% "spray-testkit" % sprayV % "test",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV % "test"
  )
}

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )
