name := "Abstract Interpretation Framework"

version := "0.0.1-SNAPSHOT"

//scalacOptions in Compile := Seq("-deprecation", "-feature", "-unchecked", "-Xlint", "-Xdisable-assertions")
//scalacOptions in Compile += "-Xdisable-assertions"

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - Abstract Interpretation Framework")

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.1"

////////////////////// "run" Configuration

fork in run := true

javaOptions in run := Seq("-Xmx2G", "-Xms1024m", "-XX:NewRatio=1", "-XX:SurvivorRatio=8", "-XX:+UseParallelGC", "-XX:+AggressiveOpts", "-Xnoclassgc")


////////////////////// (Unit) Tests

parallelExecution in Test := true

fork in Test := false


////////////////////// Integration Tests

logBuffered in IntegrationTest := false

