package model

import java.util.concurrent.Executors

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/**
  * Custom execution context.
  */
object CustomExecutionContext {

  implicit val executionContextExecutor: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(20))

}
