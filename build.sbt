name := """bidchamp"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"


val miscDependencies = Seq(
  "com.google.api-client" % "google-api-client" % "1.22.0",
  "com.jason-goodwin" %% "authentikat-jwt" % "0.4.1",
  "com.pauldijou" %% "jwt-play-json" % "0.9.0"
)

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)


