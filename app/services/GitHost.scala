package services

/**
  * Represents a Git hosting service.
  */
trait GitHost {

  /**
    * Get the Git groups.
    * e.g.: a user for GitHub, or a group ID for GitLab.
    */
  def getGroups: Seq[String]

  /**
    * Get the Git repositories.
    */
  def getRepositories: Seq[GitRepository]
}

/**
  * Wrapper around a Git repository [[url]], given a [[user]] accessing it with a [[token]].
  */
case class GitRepository(url: String, user: String, token: String) {
  override def toString = s"GitRepository($url,$user)"
}
