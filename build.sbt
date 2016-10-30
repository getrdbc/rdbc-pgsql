import de.heikoseeberger.sbtheader.license.Apache2_0

lazy val commonSettings = Seq(
  organization := "io.rdbc.pgsql",
  version := "0.0.1",
  scalaVersion := "2.11.8",
  scalacOptions ++= Vector(
    "-unchecked",
    "-deprecation",
    "-language:_",
    "-target:jvm-1.8",
    "-encoding", "UTF-8"
  ),
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  bintrayOrganization := Some("rdbc"),
  headers := Map(
    "scala" -> Apache2_0("2016", "Krzysztof Pado")
  )
)

lazy val rdbcPgsql = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    publishArtifact := false,
    bintrayReleaseOnPublish := false
  )
  .aggregate(core, scodec, nettyBackend)

lazy val core = (project in file("rdbc-pgsql-core"))
  .settings(commonSettings: _*)
  .settings(
    name := "pgsql-core",
    libraryDependencies ++= Vector(
      Library.rdbcScalaApi,
      Library.rdbcTypeconv,
      Library.rdbcImplbase,
      Library.typesafeConfig,
      Library.stm
    )
  )

lazy val scodec = (project in file("rdbc-pgsql-scodec"))
  .settings(commonSettings: _*)
  .settings(
    name := "pgsql-codec-scodec",
    libraryDependencies ++= Vector(
      Library.scodecBits,
      Library.scodecCore
    )
  ).dependsOn(core)

lazy val nettyBackend = (project in file("rdbc-pgsql-netty"))
  .settings(commonSettings: _*)
  .settings(
    name := "pgsql-netty-backend",
    libraryDependencies ++= Vector(
      Library.nettyHandler,
      Library.nettyEpoll,
      Library.rdbcTypeconv,
      Library.stm,
      Library.scalaLogging,
      Library.logback
    )
  ).dependsOn(core, scodec)

lazy val playground = (project in file("rdbc-pgsql-playground"))
  .settings(commonSettings: _*)
  .settings(
    name := "pgsql-playground",
    publishArtifact := false,
    bintrayReleaseOnPublish := false
  ).dependsOn(core, scodec, nettyBackend)