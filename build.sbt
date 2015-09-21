lazy val sharedSettings = Seq(
  scalaVersion := "2.11.7",
  resolvers += Resolver.sonatypeRepo("snapshots")
)

lazy val root = Project(
  id = "root",
  base = file("root")
) aggregate (
  scrutinee,
  explorer
)

lazy val scrutinee = Project(
  id = "scrutinee",
  base = file("scrutinee")
) settings (
  sharedSettings: _*
) settings (
  addCompilerPlugin("org.scalameta" % "scalahost" % "0.1.0-SNAPSHOT" cross CrossVersion.full),
  sourceDirectory in Compile := {
    val defaultValue = (sourceDirectory in Test).value
    System.setProperty("sbt.paths.scrutinee.sources", defaultValue.getAbsolutePath)
    defaultValue
  },
  fullClasspath in Compile := {
    val defaultValue = (fullClasspath in Compile).value
    val classpath = defaultValue.files.map(_.getAbsolutePath)
    val scalaLibrary = classpath.map(_.toString).find(_.contains("scala-library")).get
    System.setProperty("sbt.paths.scalalibrary.classes", scalaLibrary)
    System.setProperty("sbt.paths.scrutinee.classes", classpath.mkString(java.io.File.pathSeparator))
    defaultValue
  }
)

lazy val explorer = Project(
  id = "explorer",
  base = file("explorer")
) settings (
  sharedSettings: _*
) settings (
  libraryDependencies += "org.scalameta" % "scalahost" % "0.1.0-SNAPSHOT" cross CrossVersion.full
)
