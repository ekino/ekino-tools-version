package filters

import javax.inject.Inject
import play.api.http.DefaultHttpFilters
import play.api.http.EnabledFilters

/**
  * Http Filter Configuration.
  */
class Filters @Inject() (
  defaultFilters: EnabledFilters,
  cacheFilter: CacheFilter
) extends DefaultHttpFilters(defaultFilters.filters :+ cacheFilter: _*)
