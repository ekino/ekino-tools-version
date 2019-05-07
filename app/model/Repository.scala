package model

/**
  * Repository.
  */
case class Repository(name: String,
                      group: String,
                      dependencies: Seq[Dependency],
                      toolVersion: String,
                      plugins: Seq[Plugin])
