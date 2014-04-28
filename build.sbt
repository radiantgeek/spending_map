name := "clearspending"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "org.webjars" %% "webjars-play" % "2.2.1-2",
  "org.webjars" % "bootstrap" % "3.1.1",
  "org.webjars" % "font-awesome" % "4.0.3",
  "org.webjars" % "highcharts" % "3.0.9",
  "org.webjars" % "jquery" % "2.1.0",
  "org.webjars" % "leaflet" % "0.7.2",
  "org.webjars" % "typeahead.js" % "0.9.3",
  "com.logentries" % "logentries-appender" % "1.1.20",
  "com.newrelic.agent.java" % "newrelic-api" % "3.5.1"
)

play.Project.playScalaSettings
