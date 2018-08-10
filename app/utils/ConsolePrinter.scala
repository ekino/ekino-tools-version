package utils

import model.Repository

/**
  * Old console printer.
  * No more used.
  */
object ConsolePrinter {

  val line: String = "-" * 155

  // print the repositories
  def printResults(repositories: Seq[Repository], mergedValues: Map[String, String], mvnValues: Map[String, String]) {
    repositories
      .sortBy(_.name)
      .foreach(r => {
        println(Console.YELLOW + line)
        println(Console.WHITE + Console.BOLD + (" " * 20) + Console.UNDERLINED + r.name + Console.RESET + (" " * (50 - r.name.length)) + "current repository" + " " * 15 +
          "more recent in project" + " " * 10 + "latest in mvn central")
        println(Console.YELLOW + line)
        r.versions
          .toSeq
          .sortBy(_._1)
          .foreach(l => {
            val out1 = 70 - l._1.length
            val out2 = 30 - l._2.length
            var out3 = 30

            if (l._2 != mergedValues(l._1)) {
              val message = mergedValues(l._1)
              out3 = 30 - message.length
              val mvnColor = if (mvnValues(l._1) == l._2) Console.GREEN else Console.BLUE
              println(Console.WHITE + l._1 + (" " * out1) + Console.YELLOW + l._2 + (" " * out2) + Console.GREEN + message + (" " * out3) + mvnColor + mvnValues(l._1))
            } else {
              val message = mergedValues(l._1)
              out3 = 30 - message.length
              val mvnColor = if (mvnValues(l._1) == l._2) Console.GREEN else Console.BLUE
              println(Console.WHITE + l._1 + (" " * out1) + Console.GREEN + l._2 + (" " * 30) + (" " * out3) + mvnColor + mvnValues(l._1))
            }
          })
        println(Console.YELLOW + line)
        println("")
        println("")

      })
  }

}
