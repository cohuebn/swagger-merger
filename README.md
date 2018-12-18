# Swagger Merger Plugin
This SBT plugin can be used to take multiple Swagger source files (in yaml format)
and merge them into a single Swagger file

## Using the plugin
1. ~~Ensure the repository is in the list of resolvers for the project~~
As of right now, there is no repository for this plugin; you must
pull the source for the plugin and publish yourself (e.g. ```sbt publishLocal```)
1. Add the Swagger merger plugin to your list of plugins (generally in the
```plugins.sbt``` project file:
```addSbtPlugin("com.bayer.company360" % "swagger-merger" % "0.0.1-SNAPSHOT")```
1. Enable the plugin for the project. The example below shows how to enable the
plugin as part of the 'root' project:
```lazy val root = (project in file(".")).enablePlugins(SwaggerMergerPlugin)```
1. The plugin is now ready for use (with default settings). Use the command
```sbt mergeSwagger``` to run the merge task.

## Configurable options
The following options can be set on any project using the swagger merger plugin:
| Option | Type | Description | Default |
| ------ | ---- | ----------- | ------- |
| mergeSwaggerInputFilter | String | Glob-style string that indicates which source files should be included in the merge | *.yaml |