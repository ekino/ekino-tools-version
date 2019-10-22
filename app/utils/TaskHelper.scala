package utils

import scalaz.concurrent.Task

import scala.collection.Iterable

object TaskHelper {
  def gather[T](values: Iterable[Task[T]]): Task[List[T]] = Task.gatherUnordered(values.toSeq)
}
