// Scala
val log4catsV = "1.3.1"
val kafka4sV = "3.0.0-M30"
val fs2KafkaV = "1.7.0"
val http4sV = "0.21.24"
val pureConfigV = "0.16.0"
val testcontainersV = "0.39.5"
val munitV = "0.7.20"
val munitCatsEffectV = "0.13.0"

// Java
val logbackClassicV = "1.2.3"

//plugins
val kindProjectorV = "0.13.0"
val bm4V = "0.3.1"

val sharedSettings = Seq(
  scalaVersion := "2.13.6",
  Test / fork := true,
  resolvers += "confluent" at "https://packages.confluent.io/maven/",
  addCompilerPlugin("org.typelevel" % "kind-projector"      % kindProjectorV cross CrossVersion.full),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % bm4V),
  testFrameworks += new TestFramework("munit.Framework")
)

val commonDeps =
  Seq(
    "ch.qos.logback"        % "logback-classic"             % logbackClassicV,
    "org.typelevel"         %% "log4cats-slf4j"             % log4catsV,
    "org.http4s"            %% "http4s-dsl"                 % http4sV,
    "org.http4s"            %% "http4s-blaze-server"        % http4sV,
    "com.github.pureconfig" %% "pureconfig"                 % pureConfigV,
    "com.github.pureconfig" %% "pureconfig-http4s"          % pureConfigV,
    "org.scalameta"         %% "munit"                      % munitV % Test,
    "org.typelevel"         %% "munit-cats-effect-2"        % munitCatsEffectV % Test,
    "com.dimafeng"          %% "testcontainers-scala-munit" % testcontainersV % Test,
    "com.dimafeng"          %% "testcontainers-scala-kafka" % testcontainersV % Test
  )

val fs2kafkaDeps = Seq("com.github.fd4s" %% "fs2-kafka-vulcan" % fs2KafkaV)

val kafka4sDeps = Seq("com.banno" %% "kafka4s" % kafka4sV)

lazy val common =
  project
    .in(file("modules/common"))
    .settings(name := "common")
    .settings(sharedSettings)
    .settings(libraryDependencies ++= commonDeps)

lazy val fs2kafka =
  project
    .in(file("modules/fs2kafka"))
    .settings(name := "fs2kafka")
    .settings(sharedSettings)
    .settings(libraryDependencies ++= fs2kafkaDeps)
    .dependsOn(common % "compile->compile;test->test")

lazy val kafka4s =
  project
    .in(file("modules/kafka4s"))
    .settings(name := "kafka4s")
    .settings(sharedSettings)
    .settings(libraryDependencies ++= kafka4sDeps)
    .dependsOn(common % "compile->compile;test->test")

lazy val root =
  project
    .in(file("."))
    .settings(name := "kafka-spike")
    .aggregate(common, fs2kafka, kafka4s)
