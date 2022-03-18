package utils

import org.junit.Test
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.AssertionsForJUnit

class YarnRepositoryParserTest extends AssertionsForJUnit with Matchers {

  @Test
  def should_parse_lock_file_v1(): Unit = {
    val prop =
      """"@ampproject/remapping@1.0.2":
        |  version "1.0.2"
        |  resolved "https://@ampproject/remapping/-/remapping-1.0.2.tgz#a7ebbadb71517dd63298420868f27d98fe230a0a"
        |  integrity sha1-p+u623FRfdYymEIIaPJ9mP4jCgo=
        |""".stripMargin
    val matchers = YarnLockRepositoryParser.yarnVersion.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "@ampproject/remapping"
    matchers.group(2) shouldBe "1.0.2\""
    matchers.group(3) shouldBe "1.0.2"
  }

  @Test
  def should_parse_lock_file_v6(): Unit = {
    val prop =
      """"@elastic/elasticsearch@npm:6.8.8":
        |  version: 6.8.8
        |  resolution: "@elastic/elasticsearch@npm:6.8.8::__archiveUrl=https://elasticsearch-6.8.8.tgz"
        |  dependencies:
        |    debug: ^4.1.1
        |    decompress-response: ^4.2.0
        |    into-stream: ^5.1.0
        |    ms: ^2.1.1
        |    once: ^1.4.0
        |    pump: ^3.0.0
        |    secure-json-parse: ^2.1.0
        |  checksum: 066ac826cf317ed72ef6f05cd991c0f31a5788f9f95ba4972072e4af9cd465310ecc72168c10ce6935
        |  languageName: node
        |  linkType: hard
        |""".stripMargin
    val matchers = YarnLockRepositoryParser.yarnVersion.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "@elastic/elasticsearch"
    matchers.group(2) shouldBe "npm:6.8.8\""
    matchers.group(3) shouldBe "6.8.8"
  }
}
