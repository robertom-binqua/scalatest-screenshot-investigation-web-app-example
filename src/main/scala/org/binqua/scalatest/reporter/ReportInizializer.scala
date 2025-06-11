package org.binqua.scalatest.reporter

import java.time.Clock

trait ReportInitializer {
  def init(): Either[String, ReportFileUtils]
}

class ReportInitializerImpl extends ReportInitializer {
  override def init(): Either[String, ReportFileUtils] = TestsCollectorConfigurationFactory
    .create(systemPropertyReportDestinationKey = "reportDestinationRoot", fixedClock = Clock.systemUTC())
    .map(config => new ReportFileUtilsImpl(config))
}