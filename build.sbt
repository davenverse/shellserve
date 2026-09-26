import org.typelevel.sbt.gha.{PermissionValue, Permissions}

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

// npm trusted publishing (OIDC) needs Node >= 22.14 and npm >= 11.5.1, so the
// old setup-node@v1 on Node 14 is far below the floor.
val NodeVersion = "24"

ThisBuild / githubWorkflowBuildPreamble ++= Seq(WorkflowStep.Use(
  UseRef.Public("actions", "setup-node", "v6"),
  Map(
    "node-version" -> NodeVersion,
    "registry-url" -> "https://registry.npmjs.org"
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
    UseRef.Public("actions", "setup-node", "v6"),
    Map(
      "node-version" -> NodeVersion,
      "registry-url" -> "https://registry.npmjs.org"
    )
  ),
  // Node 24 does not always ship npm >= 11.5.1, which is the floor for trusted
  // publishing. Pinning to latest keeps this independent of what Node bundles.
  WorkflowStep.Run(
    List("npm install -g npm@latest"),
    name = Some("Upgrade npm for trusted publishing")
  )
)


ThisBuild / githubWorkflowPublish ++= Seq(
  WorkflowStep.Sbt(
    List("npmPackagePublish"),
    name = Some("Publish artifacts to npm"),
    cond = Some("github.event_name != 'pull_request' && (startsWith(github.ref, 'refs/tags/v'))")
  )
)

// Trusted publishing authenticates over OIDC, so the publish job needs an id
// token. contents:read is for the checkout and actions:read is for
// download-artifact, which is invoked with an explicit run-id and so goes
// through the API rather than the run-local artifact service.
ThisBuild / githubWorkflowGeneratedCI ~= {
  _.map { job =>
    if (job.id == "publish")
      job.withPermissions(
        Some(
          Permissions.Specify.defaultRestrictive
            .withActions(PermissionValue.Read)
            .withIdToken(PermissionValue.Write)
        )
      )
    else job
  }
}

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
    // Defaults to the git remote, which is the ssh form locally and the https
    // form in CI. Trusted publishing matches package.json's repository against
    // the GitHub repo, so pin it rather than let it vary by checkout.
    npmPackageRepository := Some("https://github.com/davenverse/shellserve"),
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
