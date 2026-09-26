ThisBuild / tlBaseVersion := "0.0" // your current series x.y

ThisBuild / organization := "io.chrisdavenport"
ThisBuild / organizationName := "Christopher Davenport"
ThisBuild / licenses := Seq(License.MIT)
ThisBuild / developers := List(
  tlGitHubDev("christopherdavenport", "Christopher Davenport")
)
ThisBuild / tlCiReleaseBranches := Seq()


val Scala213 = "2.13.18"

ThisBuild / crossScalaVersions := Seq(Scala213)
ThisBuild / scalaVersion := Scala213

ThisBuild / testFrameworks += new TestFramework("munit.Framework")

ThisBuild / githubWorkflowBuildPreamble ++= Seq(WorkflowStep.Use(
  UseRef.Public("actions", "setup-node", "v1"),
  Map(
    "node-version" -> "14"
  ),
  cond = Some("matrix.project == 'rootJS'")
))

ThisBuild / githubWorkflowBuild ++= Seq(
  WorkflowStep.Sbt(
    List("npmPackageInstall"),
    name = Some("Install artifacts to npm"),
    cond = Some("matrix.project == 'rootJS'")
  )
)

ThisBuild / githubWorkflowPublishPreamble ++= Seq(
  WorkflowStep.Use(
    UseRef.Public("actions", "setup-node", "v1"),
    Map(
      "node-version" -> "14",
    ),
  )
)


ThisBuild / githubWorkflowPublish ++= Seq(
  WorkflowStep.Sbt(
    List("coreJS/npmPackageNpmrc", "npmPackagePublish"),
    name = Some("Publish artifacts to npm"),
    env = Map(
      "NPM_TOKEN" -> "${{ secrets.NPM_TOKEN }}" // https://docs.npmjs.com/using-private-packages-in-a-ci-cd-workflow#set-the-token-as-an-environment-variable-on-the-cicd-server
    ),
    cond = Some("github.event_name != 'pull_request' && (startsWith(github.ref, 'refs/tags/v'))")
  )
)

ThisBuild / tlCiMimaBinaryIssueCheck  := false
ThisBuild / tlMimaPreviousVersions := Set.empty
// ThisBuild / mimaPreviousArtifacts := Set()

val catsV = "2.13.0"
val catsEffectV = "3.7.1"
val fs2V = "3.14.0"
val http4sV = "0.23.37"
val circeV = "0.14.2"
val doobieV = "1.0.0-RC2"
val munitCatsEffectV = "2.2.1"


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
      "io.chrisdavenport"           %%% "process" % "0.2.0",

      "org.typelevel"               %%% "munit-cats-effect"        % munitCatsEffectV         % Test,

    )
  ).jsConfigure(
    _.enablePlugins(NpmPackagePlugin)
  ).jsSettings(
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule)},
    npmPackageAuthor := "Christopher Davenport",
    npmPackageDescription := "shellserve is used to easily give shells scripts http access similar to cgi-bin but from command line.",
    npmPackageKeywords := Seq(
      "http",
      "shell",
    ),
    npmPackageBinaryEnable := true,
    scalaJSUseMainModuleInitializer := true,

    npmPackageStage := org.scalajs.sbtplugin.Stage.FullOpt,
    npmPackageAdditionalNpmConfig := {
      Map(
        "bin" -> _root_.io.circe.Json.obj(
          "shellserve" -> _root_.io.circe.Json.fromString("main.js")
        )
      )
    }
  )

lazy val site = project.in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    laikaTheme := tlSiteHelium.value.site
      .topNavigationBar(
        homeLink = laika.helium.config.IconLink.internal(laika.ast.Path.Root / "index.md", laika.helium.config.HeliumIcon.home)
      )
      .build
  )
  .dependsOn(core.jvm)
