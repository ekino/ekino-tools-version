package utils

import org.junit.Test
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.AssertionsForJUnit

class MavenRepositoryParserTest extends AssertionsForJUnit with Matchers  {

  @Test
  def should_parse_dependencies(): Unit = {
    val prop = """
                <dependency>
                    <groupId>org.projectlombok</groupId>
                    <artifactId>lombok</artifactId>
                    <version>1.16.14</version>
                    <scope>provided</scope>
                </dependency>
               """
    val matchers = MavenRepositoryParser.artifactRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "org.projectlombok"
    matchers.group(2) shouldBe "lombok"
    matchers.group(3) shouldBe "1.16.14"
  }

  @Test
  def should_parse_properties(): Unit = {
    val prop = """
                <properties>
                    <spring.version>4.3.2.RELEASE</spring.version>
                </properties>
               """
    val matchers = MavenRepositoryParser.propertyRegex.findAllIn(prop)

    matchers should not be empty
    matchers.group(1) shouldBe "spring.version"
    matchers.group(2) shouldBe "4.3.2.RELEASE"
  }
}
