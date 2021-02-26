package model

/**
  * Repository.
  */
case class Repository(name: String,
                      group: String,
                      dependencies: collection.Seq[Dependency],
                      toolVersion: String,
                      plugins: collection.Seq[Plugin])
