import java.util.concurrent.{ExecutorService, ForkJoinPool}

package object executors {
  implicit val pool: ExecutorService = new ForkJoinPool(20)
}
