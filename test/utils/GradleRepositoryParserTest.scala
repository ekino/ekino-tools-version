package utils

import org.junit.Test
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.AssertionsForJUnit

class GradleRepositoryParserTest extends AssertionsForJUnit with Matchers  {

  @Test
  def should_parse_ekino_style_dependencies(): Unit = {
    val prop = "compile group: 'com.ekino.base', name: 'ekino-base-service', version: property('ekino-base-service.version')"
    val matchers = GradleRepositoryParser.artifactRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "com.ekino.base"
    matchers.group(2) shouldBe "ekino-base-service"
    matchers.group(3) shouldBe "ekino-base-service.version"
  }

  @Test
  def should_parse_ekino_style_dependencies_with_special_characters(): Unit = {
    val prop = """playTest group: 'org.scalatestplus.play', name: 'scalatestplus-play_2.11', version: property('scalatestplus-play_2.11.version')"""
    val matchers = GradleRepositoryParser.artifactRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "org.scalatestplus.play"
    matchers.group(2) shouldBe "scalatestplus-play_2.11"
    matchers.group(3) shouldBe "scalatestplus-play_2.11.version"
  }

  @Test
  def should_parse_other_style_dependencies(): Unit = {
    val prop = """  compile("com.ekino.library:ekino-library-logs:${ekinoLogsVersion}")"""
    val matchers = GradleRepositoryParser.artifactRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "com.ekino.library"
    matchers.group(2) shouldBe "ekino-library-logs"
    matchers.group(3) shouldBe "ekinoLogsVersion"
  }

  @Test
  def should_parse_maven_bom(): Unit = {
    val prop = """mavenBom 'org.springframework.cloud:spring-cloud-dependencies:Dalston.SR4'"""
    val matchers = GradleRepositoryParser.artifactRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "org.springframework.cloud"
    matchers.group(2) shouldBe "spring-cloud-dependencies"
    matchers.group(3) shouldBe "Dalston.SR4"
  }

  @Test
  def should_parse_simple_kotlin_style_dependencies(): Unit = {
    val prop = """  api("com.ekino.base:ekino-base-service:1.0.0")"""
    val matchers = GradleRepositoryParser.artifactRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "com.ekino.base"
    matchers.group(2) shouldBe "ekino-base-service"
    matchers.group(3) shouldBe "1.0.0"
  }

  @Test
  def should_parse_kotlin_style_dependencies(): Unit = {
    val prop = """  compile("com.ekino.base:ekino-base-service:" + property("ekino-base-service.version"))"""
    val matchers = GradleRepositoryParser.artifactRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "com.ekino.base"
    matchers.group(2) shouldBe "ekino-base-service"
    matchers.group(3) shouldBe "ekino-base-service.version"
  }

  @Test
  def should_parse_kotlin_style_dependencies_with_named_parameters(): Unit = {
    val prop = """ testCompile (group= "org.assertj", name = "assertj-core", version = property("assertj.version") as String)"""
    val matchers = GradleRepositoryParser.artifactRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "org.assertj"
    matchers.group(2) shouldBe "assertj-core"
    matchers.group(3) shouldBe "assertj.version"
  }

  @Test
  def should_parse_gradle_platform_dependencies(): Unit = {
    val prop = """ implementation(platform("org.springframework.boot:spring-boot-dependencies:2.2.1.RELEASE"))"""
    val matchers = GradleRepositoryParser.artifactRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "org.springframework.boot"
    matchers.group(2) shouldBe "spring-boot-dependencies"
    matchers.group(3) shouldBe "2.2.1.RELEASE"
  }

  @Test
  def should_parse_gradle_enforced_platform_dependencies(): Unit = {
    val prop = """ implementation(enforcedPlatform("org.springframework.boot:spring-boot-dependencies:2.2.1.RELEASE"))"""
    val matchers = GradleRepositoryParser.artifactRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "org.springframework.boot"
    matchers.group(2) shouldBe "spring-boot-dependencies"
    matchers.group(3) shouldBe "2.2.1.RELEASE"
  }

  @Test
  def should_parse_plugins(): Unit = {
    val prop = """ id 'com.ekino.base'   version '1.2.0' """
    val matchers = GradleRepositoryParser.pluginRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "com.ekino.base"
    matchers.group(2) shouldBe "1.2.0"
  }

  @Test
  def should_parse_plugins_with_kotlin_script(): Unit = {
    val prop = """ id ("com.ekino.base")     version "1.2.0" """
    val matchers = GradleRepositoryParser.pluginRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "com.ekino.base"
    matchers.group(2) shouldBe "1.2.0"
  }

  @Test
  def should_parse_plugins_with_property(): Unit = {
    val prop =
      """ val baseVersion = "1.2.3"
        | id 'com.ekino.base'   version baseVersion
      """.stripMargin
    val matchers = GradleRepositoryParser.pluginRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "com.ekino.base"
    matchers.group(2) shouldBe "baseVersion"
  }

  @Test
  def should_parse_plugins_with_kotlin_script_without_space_after_id(): Unit = {
    val prop = """ id("com.ekino.base")     version "1.2.0" """
    val matchers = GradleRepositoryParser.pluginRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "com.ekino.base"
    matchers.group(2) shouldBe "1.2.0"
  }

}
