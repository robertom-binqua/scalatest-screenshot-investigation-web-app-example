package org.binqua.scalatest.reporter

import cats.implicits.catsSyntaxOptionId
import org.binqua.scalatest.reporter.util.utils.EitherOps

import java.util.concurrent.locks.{Condition, ReentrantLock}

trait WebDriverTestsCollector {
  def addScreenshot(screenshotDriverData: ScreenshotDriverData): Unit
}

trait ReporterTestsCollector {
  def add(event: StateEvent): List[StateEvent]
}

trait TestsCollector extends WebDriverTestsCollector with ReporterTestsCollector

object TestsCollector {

  private val internalTestsCollector: TestsCollector = new TestsCollectorImpl()

  val webDriverTestsCollector: WebDriverTestsCollector = internalTestsCollector

  val reporterTestsCollector: ReporterTestsCollector = internalTestsCollector

}

// ThreadSafe
final class TestsCollectorImpl extends TestsCollector {

  private val lock: ReentrantLock = new ReentrantLock()
  private val screenshotTakenWeCanProceedConsumingEvents: Condition = lock.newCondition()
  private val lastEventIsTakeAScreenshot: Condition = lock.newCondition()

  // guarded by lock
  private var keepReadingAllEvents: Boolean = true
  // guarded by lock
  private var lastEvent: Option[StateEvent] = None
  // guarded by lock
  private var events: Vector[StateEvent] = Vector.empty

  // blocks-until keepReadingAllEvents = false, to give time to addScreenshot to read the right test coordinates.
  def add(event: StateEvent): List[StateEvent] = {
    def internalAdd(event: StateEvent): List[StateEvent] = {
      lock.lock();
      try {
        while (!keepReadingAllEvents)
          screenshotTakenWeCanProceedConsumingEvents.await()

        lastEvent = event.some

        events = events :+ event

        if (eventIsTakeAScreenshotEvent(lastEvent)) {
          keepReadingAllEvents = false // block the current method on the dispatcher thread when we exit, so add screenshot will be the only thread
          lastEventIsTakeAScreenshot.signalAll()
        }

        events.toList

      } finally {
        lock.unlock()
      }
    }
    internalAdd(event)
  }

  // blocks-until lastEventIsNot -> TakeAScreenshotEvent = StateEvent.Note(_, message, _, _) => message.startsWith("take screenshot now")
  def addScreenshot(screenshotDriverData: ScreenshotDriverData): Unit = {
    lock.lock()
    try {
      while (!eventIsTakeAScreenshotEvent(lastEvent)) // events are still too old: waiting to reach "take a screenshot event"
        lastEventIsTakeAScreenshot.await()

      events = events :+ StateEvent
        .extractRunningScenarioFrom(lastEvent)
        .map(runningScenario => StateEvent.Screenshot(runningScenario, screenshotDriverData, 0))
        .getOrThrow

      keepReadingAllEvents = true
      lastEvent = None
      screenshotTakenWeCanProceedConsumingEvents.signalAll()
    } finally {
      lock.unlock()
    }

  }

  private def eventIsTakeAScreenshotEvent(event: Option[StateEvent]): Boolean = event match {
    case Some(StateEvent.Note(_, message, _, _)) => message.startsWith("take screenshot now")
    case _                                       => false
  }

}
