package com.bayer.company360.swagger

import sbt._
import Keys._
import AppInjector.injector
import scaldi.Injectable._

object SwaggerMergerPlugin extends AutoPlugin {
  object autoImport {
    val mergeSwaggerInputFilter = settingKey[FileFilter]("Determines which files should be merged together")
    val mergeSwaggerPrintInputs = settingKey[Boolean]("Should all located input files be printed before generating output?")
    val mergeSwaggerOutputFilename = settingKey[String]("The name of the generated Swagger file")
    val mergeSwagger = inputKey[Unit]("Merges multiple Swagger documents into one")
  }

  import autoImport._
  override lazy val projectSettings = Seq(
    mergeSwaggerInputFilter := "*.yaml",
    mergeSwaggerPrintInputs := false,
    mergeSwaggerOutputFilename := "api.swagger.yaml",
    mergeSwagger := {
      val filteredFiles = (baseDirectory.value ** mergeSwaggerInputFilter.value).get
      if (mergeSwaggerPrintInputs.value) {
        println(s"The following files will be merged into ${mergeSwaggerOutputFilename.value}")
        filteredFiles.foreach(x => println(s"\t- ${x.getCanonicalPath}"))
      }

      val generatedFile = (resourceManaged in mergeSwagger).value / mergeSwaggerOutputFilename.value
      val generatedContents = inject[SwaggerMerger].mergeFiles(filteredFiles)
      IO.write(generatedFile, generatedContents)
      println(s"Generated file ${generatedFile}")
    }
  )
}