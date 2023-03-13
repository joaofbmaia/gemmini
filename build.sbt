// See README.md for license details.

scalacOptions ++= Seq(
    "-Xsource:2.11"
)

name := "gemmini"

version := "3.1.0"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "edu.berkeley.cs" %% "chisel3" % "3.4.+",
  "edu.berkeley.cs" %% "rocketchip" % "1.2.+",
  "edu.berkeley.cs" %% "chisel-iotesters" % "1.5.+",
  "edu.berkeley.cs" %% "chiseltest" % "0.5.4" % "test", 
  "org.scalanlp" %% "breeze" % "1.1")

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases"),
  Resolver.mavenLocal)
