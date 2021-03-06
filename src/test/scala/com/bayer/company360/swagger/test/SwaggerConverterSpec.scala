package com.bayer.company360.swagger.test

import java.io.File

import com.bayer.company360.swagger.{FileException, SwaggerConverter}
import com.bayer.company360.swagger.SwaggerSchema.{PathName, SwaggerDoc}
import io.circe.{DecodingFailure, ParsingFailure}

import scala.util.matching.Regex.Groups

class SwaggerConverterSpec extends Spec {
  trait Setup {
    val swaggerConverter = new SwaggerConverter

    def parseValidFile(file: File): SwaggerDoc = {
      val parsed = swaggerConverter.parse(file)
      parsed.success.value
    }
  }

  "A swagger converter" when {
    "unparsable file" should {
      "return the parsing failure" in new Setup {
        val invalidYamlTestCase = getResourceAsFile("invalid-yaml.yaml")
        val parsed = swaggerConverter.parse(invalidYamlTestCase)

        parsed.failure.exception shouldBe a[FileException]
        parsed.failure.exception.getCause shouldBe a[ParsingFailure]
        parsed.failure.exception.getMessage should include("invalid-yaml.yaml")
      }
    }

    "invalid data" should {
      "return the decoding failure" in new Setup {
        val invalidDataTestCase = getResourceAsFile("invalid-data.yaml")
        val parsed = swaggerConverter.parse(invalidDataTestCase)

        parsed.failure.exception shouldBe a[FileException]
        parsed.failure.exception.getCause shouldBe a[DecodingFailure]
        parsed.failure.exception.getMessage should include("invalid-data.yaml")
      }
    }

    "swagger fields are populated" should {
      val allFieldsPopulatedTestCase = getResourceAsFile("all-fields-populated.yaml")

      "parse the swagger version" in new Setup {
        val parsed = parseValidFile(allFieldsPopulatedTestCase)
        parsed.swagger should equal("2.0")
      }

      "parse the info element" in new Setup {
        val parsed = parseValidFile(allFieldsPopulatedTestCase)
        parsed.info.title should equal("A fancy title")
        parsed.info.description should equal(Some("This API will change your life"))
        parsed.info.version should equal("v67")
      }

      "parse the host element" in new Setup {
        val parsed = parseValidFile(allFieldsPopulatedTestCase)
        parsed.host should equal("a.domain.com")
      }

      "parse the schemes element" in new Setup {
        val parsed = parseValidFile(allFieldsPopulatedTestCase)
        parsed.schemes should contain theSameElementsInOrderAs List("https", "http")
      }

      "parse the base path element" in new Setup {
        val parsed = parseValidFile(allFieldsPopulatedTestCase)
        parsed.basePath should equal("/wacky-cat-pics/v6")
      }

      "parse the max records element" in new Setup {
        val parsed = parseValidFile(allFieldsPopulatedTestCase)
        parsed.`x-what-is-maximum-number-of-records-that-could-be-returned` should equal(33)
      }

      "parse the produces element" in new Setup {
        val parsed = parseValidFile(allFieldsPopulatedTestCase)
        val expected = Seq("application/json", "image/png", "image/gif", "image/jpeg")
        parsed.produces should contain theSameElementsInOrderAs expected
      }

      "parse all paths" in new Setup {
        val parsed = parseValidFile(allFieldsPopulatedTestCase)
        val expected = Set[PathName]("/wearing-hats", "/search-the-interwebs")
        parsed.paths.keys should contain theSameElementsAs expected
      }

      "parse a 'get' path" in new Setup {
        val parsed = parseValidFile(allFieldsPopulatedTestCase)

        assertOnOption(parsed.paths.get("/wearing-hats")) { path =>
          path.get should not be None
          path.post shouldBe None

          assertOnOption(path.get) { getOperation =>
            getOperation.summary should equal(Option("Cats in hats"))
            getOperation.operationId should equal(Option("cats-in-hats"))
            getOperation.description should equal(Option("You should understand this endpoint by now"))
            assertOnOption(getOperation.tags) { tags =>
              tags should contain theSameElementsInOrderAs (Seq("kitties", "outerwear"))
            }
            getOperation.parameters.map(_.name) should contain theSameElementsInOrderAs (Seq("hat-category", "limit", "offset"))

            val parameterByName = (name: String) => getOperation.parameters.find(_.name == name)
            assertOnOption(parameterByName("hat-category")) { parameter =>
              parameter.in should equal("query")
              parameter.description should equal(Option("Only show pictures from the given hat categories"))
              parameter.required should equal(Option(true))
              parameter.`type` should equal(Option("array"))
              parameter.`x-example` should equal(Option("hat-category=flowered&hat-category=fireman"))
              assertOnOption(parameter.items) { items =>
                items.`type` should equal(Some("string"))
                items.format should equal(Some("alpha_1"))
              }
            }

            assertOnOption(parameterByName("limit")) { parameter =>
              parameter.in should equal("query")
              parameter.description should equal(Option("Limit of results returned. Defaults to 25."))
              parameter.required should equal(Option(false))
              parameter.`type` should equal(Option("integer"))
              parameter.format should equal(Option("int32"))
            }

            assertOnOption(parameterByName("offset")) { parameter =>
              parameter.in should equal("query")
              parameter.description should equal(Option("Offset of returned results. Defaults to 0."))
              parameter.required should equal(Option(false))
              parameter.`type` should equal(Option("integer"))
              parameter.format should equal(Option("int32"))
            }

            getOperation.responses.keys should contain theSameElementsAs (Seq(200, 400, 429, 500))
            val responseByStatus = (status: Int) => getOperation.responses.get(status)
            assertOnOption(responseByStatus(200)) { response =>
              response.description should equal(Option("A list of matching cats-in-hats images"))
              response.schema.`type` should equal(Option("object"))
              assertOnOption(response.schema.properties) { responseProperties =>
                assertOnOption(responseProperties.get("items")) { items =>
                  items.`type` should equal(Some("array"))
                  items.items.get.`$ref` should equal(Some("#/definitions/catListing"))
                }

                assertOnOption(responseProperties.get("limit")) { limit =>
                  limit.`type` should equal(Some("integer"))
                }
                assertOnOption(responseProperties.get("offset")) { limit =>
                  limit.`type` should equal(Some("integer"))
                }
              }

              assertOnOption(response.examples) { examples =>
                examples.keys should contain theSameElementsAs Seq("application/json")
                val jsonExample = examples.get("application/json")
                val firstItem = jsonExample.get.hcursor.downField("items").downN(0)
                firstItem.downField("url").as[String].right.value should equal("http://cats-fo-days.com/1234")
                firstItem.downField("name").as[String].right.value should equal("Whisker joe")
                firstItem.downField("views").as[Int].right.value should equal(3)
                jsonExample.get.hcursor.downField("limit").as[Int].right.value should equal(25)
                jsonExample.get.hcursor.downField("offset").as[Int].right.value should equal(0)
              }
            }

            assertOnOption(responseByStatus(400)) { response =>
              response.description should equal(Option("Missing or invalid query parameter(s)"))
              response.schema.`$ref` should equal(Option("#/definitions/error"))
            }

            assertOnOption(responseByStatus(429)) { response =>
              response.description should equal(Option("Downstream connection limit exceeded"))
              response.schema.`$ref` should equal(Option("#/definitions/error"))
            }

            assertOnOption(responseByStatus(500)) { response =>
              response.description should equal(Option("Internal server error"))
              response.schema.`$ref` should equal(Option("#/definitions/error"))
            }
          }
        }
      }

      "parse a 'post' path" in new Setup {
        val parsed = parseValidFile(allFieldsPopulatedTestCase)

        assertOnOption(parsed.paths.get("/search-the-interwebs")) { path =>
          path.get should be(None)
          path.post should not be None

          assertOnOption(path.post) { postOperation =>
            postOperation.summary should equal(Option("search the whole internet for matching pics"))
            postOperation.description should equal(Option("impressive feat of technology"))
            postOperation.operationId should equal(Option("search-the-interwebs"))
            assertOnOption(postOperation.tags) { tags =>
              tags should contain theSameElementsInOrderAs (Seq("al gore", "kitties"))
            }
            postOperation.parameters.map(_.name) should contain theSameElementsInOrderAs (Seq("search", "limit"))

            val parameterByName = (name: String) => postOperation.parameters.find(_.name == name)
            assertOnOption(parameterByName("search")) { parameter =>
              parameter.in should equal("body")
              parameter.description should equal(Option("The search criteria"))
              assertOnOption(parameter.schema) { schema =>
                schema.`$ref` should equal(Some("#/definitions/searchCriteria"))
              }
            }

            assertOnOption(parameterByName("limit")) { parameter =>
              parameter.in should equal("query")
              parameter.description should equal(Option("Limit of results returned. Defaults to 25."))
              parameter.required should equal(Option(false))
              parameter.`type` should equal(Option("integer"))
              parameter.format should equal(Option("int32"))
            }

            postOperation.responses.keys should contain theSameElementsAs (Seq(200, 500))
            val responseByStatus = (status: Int) => postOperation.responses.get(status)
            assertOnOption(responseByStatus(200)) { response =>
              response.description should equal(Option("A list of matching cats images"))
              response.schema.`type` should equal(Option("object"))
              assertOnOption(response.schema.properties) { responseProperties =>
                assertOnOption(responseProperties.get("items")) { items =>
                  items.`type` should equal(Some("array"))
                  items.items.get.`$ref` should equal(Some("#/definitions/catListing"))
                }

                assertOnOption(responseProperties.get("limit")) { limit =>
                  limit.`type` should equal(Some("integer"))
                }
              }
            }

            assertOnOption(responseByStatus(500)) { response =>
              response.description should equal(Option("Internal server error"))
              response.schema.`$ref` should equal(Option("#/definitions/error"))
            }
          }
        }
      }

      "parse definitions" in new Setup {
        val parsed = parseValidFile(allFieldsPopulatedTestCase)

        parsed.definitions.keys should contain theSameElementsAs(Seq("catListing", "error"))

        val definitionByName = (name: String) => parsed.definitions.get(name)
        assertOnOption(definitionByName("catListing")) { definition =>
          definition.`type` should equal(Some("object"))
          assertOnOption(definition.properties) { properties =>
            properties.keys should contain theSameElementsAs(Seq("url", "name", "views"))

            assertOnOption(properties.get("url")) { url => {
              url.`type` should equal(Some("string"))
              url.description should equal(Some("The url for the cat pic"))
            }}

            assertOnOption(properties.get("name")) { url => {
              url.`type` should equal(Some("string"))
              url.description should equal(Some("The name of the cat"))
            }}

            assertOnOption(properties.get("views")) { url => {
              url.`type` should equal(Some("number"))
              url.description should equal(Some("The number of times this picture has been viewed"))
            }}
          }
        }

        assertOnOption(definitionByName("error")) { definition =>
          definition.`type` should equal(Some("object"))
          assertOnOption(definition.properties) { properties =>
            properties.keys should contain theSameElementsAs(Seq("status", "code", "errorMessage"))

            assertOnOption(properties.get("status")) { url => {
              url.`type` should equal(Some("string"))
            }}

            assertOnOption(properties.get("code")) { url => {
              url.`type` should equal(Some("integer"))
              url.format should equal(Some("int32"))
            }}

            assertOnOption(properties.get("errorMessage")) { url => {
              url.`type` should equal(Some("string"))
            }}
          }
        }
      }
    }

    "using simple data" should {
      val simpleTestCase = getResourceAsFile("simple.yaml")

      "represent numbers without scientific notation" in new Setup {
        val parsed = parseValidFile(simpleTestCase)
        val encoded = swaggerConverter.toYaml(parsed)

        Seq(
          ("should-not-be-scientific", "1610"),
          ("tiny-number", "0.0000001"),
          ("big-number", "1234500000000"),
          ("tiny-negative-number", "-0.0000001"),
          ("big-negative-number", "-1234500000000")
        ).foreach {
          case (fieldName: String, expected: String) =>
            val valueLocator = s"$fieldName: (.*)".r
            valueLocator.findFirstMatchIn(encoded) match {
              case Some(Groups(actual)) => actual should equal(expected)
              case _ => fail(s"Did not find expected '$fieldName' element")
            }
        }
      }
    }
  }
}
