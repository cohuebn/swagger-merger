lazy val root = (project in file("."))
  .enablePlugins(SwaggerMergerPlugin)
  .settings(
    scalaVersion := "2.12.4",
    version := "0.1",
    mergeSwaggerPrintInputs := true,
    mergeSwaggerInputFiles := baseDirectory.value ** "included-swagger" * "*.yaml",
    mergeSwaggerOutputFile :=  (resourceManaged in mergeSwagger).value / "super-special-file.swagger.yaml",
    TaskKey[Unit]("verifyFileExists") := {
      val expectedFilename = (resourceManaged.value / "super-special-file.swagger.yaml")
      if (!expectedFilename.exists())
        sys.error(s"Expected generated Swagger file to exist at $expectedFilename")
    }
  )
