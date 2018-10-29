package model

/**
  * Repository.
  */
case class Repository(
  name: String,
  group: String,
  versions: Map[String, String],
  toolVersion: String,
  plugins: Map[String, String])
