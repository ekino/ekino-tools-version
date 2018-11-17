package services

import play.api.Configuration

/**
  * Abstract definition of a [[GitHost]].
  *
  * @param repositoryName the name of the host (e.g.: gitlab, github, etc.). Must be the root of the property object in application.conf.
  * @param configuration  the application configuration.
  */
abstract class AbstractGitHost(repositoryName: String,
                               configuration: Configuration) extends GitHost {

  /**
    * Retrieve an optional representation of the Git groups.
    * e.g: a comma-separated list of IDs or users from the properties.
    */
  def getRawGroups: Option[String]

  /**
    * Retrieve a comma-separated list of groups and expose them as a sequence.
    */
  def getGroups: Seq[String] =
    getRawGroups
      .map(groups => groups.split(',').filter(_.nonEmpty).toSeq)
      .getOrElse(Seq.empty)

  protected def getIgnoredUrls: Seq[String] =
    getPropertyList(repositoryName + ".ignored-repositories")
      .getOrElse(Seq.empty)

  protected def getAdditionalUrls: Seq[String] =
    getPropertyList(repositoryName + ".additional-repositories")
      .getOrElse(Seq.empty)

  protected def getProperty(property: String): Option[String] = configuration.getOptional[String](property)

  protected def getPropertyList(property: String): Option[Seq[String]] = configuration.getOptional[Seq[String]](property)
}
