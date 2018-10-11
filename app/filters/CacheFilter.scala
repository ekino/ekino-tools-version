package filters

import akka.actor.ActorRef
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import javax.inject.{Inject, Named, Singleton}
import job.InitMessage
import play.api.mvc._
import play.api.{ConfigLoader, Configuration}
import services.VersionService

import scala.concurrent.{ExecutionContext, Future}

/**
  * Filter that inits the cache if needed.
  */
@Singleton
class CacheFilter @Inject()
  (versionService: VersionService, @Named("updater-actor") val updaterActor: ActorRef, configuration: Configuration)
  (implicit val mat: Materializer, ec: ExecutionContext) extends Filter {


  def apply(nextFilter: RequestHeader => Future[Result])
    (requestHeader: RequestHeader): Future[Result] = {

    val timeout = configuration.get("timeout.clear-cache")(ConfigLoader.finiteDurationLoader)
    ask(updaterActor, InitMessage)(Timeout(timeout))
        .flatMap(_ => nextFilter(requestHeader))

  }
}
