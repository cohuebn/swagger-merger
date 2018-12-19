package com.bayer.company360.swagger

import sbt._
import Keys._
import AppInjector.injector
import scaldi.Injectable._

import scala.util.{Failure, Success}

object SwaggerMergerPlugin extends AutoPlugin {
  object autoImport {
    val mergeSwaggerInputFiles = settingKey[PathFinder]("Determines which files should be merged together")
    val mergeSwaggerBaseFile = settingKey[PathFinder]("Which file should be used as the base file? The first file matching the pattern will be used")
    val mergeSwaggerPrintInputs = settingKey[Boolean]("Should all located input files be printed before generating output?")
    val mergeSwaggerOutputFile = settingKey[File]("The generated Swagger file")
    val mergeSwagger = inputKey[Unit]("Merges multiple Swagger documents into one")
  }

  import autoImport._
  override lazy val projectSettings = Seq(
    mergeSwaggerInputFiles := baseDirectory.value ** "*.yaml",
    mergeSwaggerBaseFile := baseDirectory.value ** "api.yaml",
    mergeSwaggerPrintInputs := false,
    mergeSwaggerOutputFile := (resourceManaged in mergeSwagger).value / "api.swagger.yaml",
    mergeSwagger := {
      val baseFile = mergeSwaggerBaseFile.value.get.head
      val filesToMerge = mergeSwaggerInputFiles.value.get.filter(file => file != baseFile)

      if (mergeSwaggerPrintInputs.value) {
        println(s"The following files will be merged to create ${mergeSwaggerOutputFile.value.getName}:")
        println(s"\t- ${baseFile.getCanonicalPath} (base file)")
        filesToMerge.foreach(x => println(s"\t- ${x.getCanonicalPath}"))
      }

      val mergedSwagger = inject[SwaggerMerger].mergeFiles(baseFile, filesToMerge)
      mergedSwagger match {
        case Failure(failure) => throw failure
        case Success(mergedSwagger) =>
          val mergedSwaggerYaml = inject[SwaggerConverter].toYaml(mergedSwagger)
          IO.write(mergeSwaggerOutputFile.value, mergedSwaggerYaml)
          println(s"Generated file ${mergeSwaggerOutputFile.value.getCanonicalPath}")
      }
    }
  )
}
