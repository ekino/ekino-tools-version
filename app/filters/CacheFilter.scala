package filters

import akka.actor.ActorRef
import akka.stream.Materializer
import javax.inject.{Inject, Named, Singleton}
import job.InitMessage
import play.api.Configuration
import play.api.mvc._
import services.VersionService

import scala.concurrent.Future

/**
  * Filter that inits the cache if needed.
  */
@Singleton
class CacheFilter @Inject()
  (versionService: VersionService, @Named("updater-actor") val updaterActor: ActorRef, configuration: Configuration)
  (implicit val mat: Materializer) extends Filter {


  def apply(nextFilter: RequestHeader => Future[Result])
    (requestHeader: RequestHeader): Future[Result] = {

    if (versionService.noData) {
      updaterActor ! InitMessage
      nextFilter(requestHeader)
    } else {
      nextFilter(requestHeader)
    }

  }
}
