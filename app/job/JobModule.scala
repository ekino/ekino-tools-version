package job

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

/**
  * JobModule.
  */
class JobModule extends AbstractModule with AkkaGuiceSupport {

  override def configure(): Unit = {
    bindActor[UpdaterActor]("updater-actor")
    bind(classOf[Scheduler]).asEagerSingleton()
  }
}
