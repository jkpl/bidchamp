name := """bidchamp"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"


val miscDependencies = Seq(
  "org.webjars" %% "webjars-play" % "2.5.0",
  "org.webjars.bower" % "material-design-lite" % "1.1.3"
)

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
) ++ miscDependencies


