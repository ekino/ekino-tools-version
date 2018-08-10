package utils

import model.ComparableVersion

/**
  * Delegate version comparison to ComparableVersion.
  */
object VersionComparator {

  // compare two versions
  def versionCompare(s1: String, s2: String): Int = {
    val a: ComparableVersion = new ComparableVersion(s1)
    val b = new ComparableVersion(s2)

    a.compareTo(b)
  }

}
