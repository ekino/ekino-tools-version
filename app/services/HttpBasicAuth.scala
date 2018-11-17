package services

import java.util.Base64

object HttpBasicAuth {
  val BASIC = "Basic"
  val AUTHORIZATION = "Authorization"

  def getHeader(username: String, password: String): String =
    BASIC + " " + encodeCredentials(username, password)

  def encodeCredentials(username: String, password: String): String =
    new String(Base64.getEncoder.encode((username + ":" + password).getBytes))
}
