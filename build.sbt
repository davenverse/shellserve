ThisBuild / tlBaseVersion := "0.0" // your current series x.y

ThisBuild / organization := "io.chrisdavenport"
ThisBuild / organizationName := "Christopher Davenport"
ThisBuild / licenses := Seq(License.MIT)
ThisBuild / developers := List(
  tlGitHubDev("christopherdavenport", "Christopher Davenport")
)
ThisBuild / tlCiReleaseBranches := Seq("main")
ThisBuild / tlSonatypeUseLegacyHost := true

val Scala213 = "2.13.7"

ThisBuild / crossScalaVersions := Seq(Scala213)
ThisBuild / scalaVersion := Scala213

ThisBuild / testFrameworks += new TestFramework("munit.Framework")

ThisBuild / tlCiMimaBinaryIssueCheck  := false
ThisBuild / tlMimaPreviousVersions := Set.empty

ThisBuild / npmPackageAuthor := "Christopher Davenport"
ThisBuild / npmPackageDescription := "shellserve is used to easily give shells scripts http access similar to cgi-bin but from command line."
ThisBuild / npmPackageKeywords := Seq(
      "http",
      "shell",
    )
ThisBuild / npmPackageBinaryEnable := true
ThisBuild / npmPackageStage := org.scalajs.sbtplugin.Stage.FullOpt
ThisBuild / npmPackageAdditionalNpmConfig := {
      Map(
        "bin" -> _root_.io.circe.Json.obj(
          "shellserve" -> _root_.io.circe.Json.fromString("main.js")
        )
      )
    }

val catsV = "2.7.0"
val catsEffectV = "3.3.12"
val fs2V = "3.2.7"
val http4sV = "0.23.12"
val circeV = "0.14.2"
val doobieV = "1.0.0-RC2"
val munitCatsEffectV = "1.0.7"


// Projects
lazy val `shellserve` = tlCrossRootProject
  .aggregate(core)

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    name := "shellserve",

    libraryDependencies ++= Seq(
      "org.typelevel"               %%% "cats-core"                  % catsV,
      "org.typelevel"               %%% "cats-effect"                % catsEffectV,

      "co.fs2"                      %%% "fs2-core"                   % fs2V,
      "co.fs2"                      %%% "fs2-io"                     % fs2V,

      "org.http4s"                  %%% "http4s-ember-server"        % http4sV,
      "io.chrisdavenport"           %%% "process" % "0.0.2",

      "org.typelevel"               %%% "munit-cats-effect-3"        % munitCatsEffectV         % Test,

    )
  ).jsConfigure(
    _.enablePlugins(NpmPackagePlugin)
  ).jsSettings(
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule)},
    scalaJSUseMainModuleInitializer := true,
  )

lazy val site = project.in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .dependsOn(core.jvm)
