package modules

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.{ScalaModule, ScalaMultibinder}
import services._

/**
  * Module enabling multi-binding of [[GitHost]] implementations as a [[scala.collection.immutable.Set]].
  */
class GitModule extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    val multi = ScalaMultibinder.newSetBinder[GitHost](binder)
    multi.addBinding.to[GitHub]
    multi.addBinding.to[GitLab]
  }
}
