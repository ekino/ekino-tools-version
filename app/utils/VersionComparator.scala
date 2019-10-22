package utils

import model.ComparableVersion

/**
  * Delegate version comparison to ComparableVersion.
  */
object VersionComparator extends Ordering[String] {

  // compare two versions
  def compare(s1: String, s2: String): Int =
    new ComparableVersion(s1) compareTo new ComparableVersion(s2)

}
