package filters

import play.api.http.DefaultHttpFilters
import play.api.http.EnabledFilters
import com.google.inject.Inject

class Filters @Inject() (
    defaultFilters: EnabledFilters,
    authFilter: AuthFilter,
) extends DefaultHttpFilters(defaultFilters.filters :+ authFilter: _*)
