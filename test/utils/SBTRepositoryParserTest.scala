package utils

import org.junit.Test
import org.scalatest.Matchers
import org.scalatestplus.junit.AssertionsForJUnit

class SBTRepositoryParserTest extends AssertionsForJUnit with Matchers {

  @Test
  def should_parse_dependencies() {
    val prop = """val poi = "org.apache.poi" % "poi" % "3.16""""
    val matchers = SBTRepositoryParser.artifactRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "org.apache.poi"
    matchers.group(2) shouldBe "poi"
    matchers.group(3) shouldBe "3.16"

  }

  @Test
  def should_parse_scala_dependencies() {
    val prop = """val pgSlick = "com.github.tminglei" %% "slick-pg" % "0.14.3""""
    val matchers = SBTRepositoryParser.scalaArtifactRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "com.github.tminglei"
    matchers.group(2) shouldBe "slick-pg"
    matchers.group(3) shouldBe "0.14.3"

  }

  @Test
  def should_parse_properties() {
    val prop = """  val precepteVersion = "0.4.4""""
    val matchers = SBTRepositoryParser.propertyRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "precepteVersion"
    matchers.group(2) shouldBe "0.4.4"

  }

  @Test
  def should_parse_scala_version() {
    val prop = """  scalaVersion := "2.11.12","""
    val matchers = SBTRepositoryParser.scalaVersionRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "2.11.12"

  }

}
