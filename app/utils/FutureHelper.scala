package utils

import model.CustomExecutionContext._
import play.api.{ConfigLoader, Configuration}

import scala.concurrent.{Await, Future}

/**
  * Future Helper.
  */
object FutureHelper {

  def await[T](seq: Iterable[Future[T]], configuration: Configuration, key: String): Seq[T] = {
    val sequence = Future.sequence(seq)

    val timeout = configuration.get(key)(ConfigLoader.finiteDurationLoader)
    Await.result(sequence, timeout).toSeq
  }

}
