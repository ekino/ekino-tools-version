package model

/**
  * Dto holding all the repositories for a project.
  */
case class Project(name: String, repositories: Seq[DisplayRepository])
