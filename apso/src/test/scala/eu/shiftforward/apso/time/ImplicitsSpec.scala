package eu.shiftforward.apso.time

import com.github.nscala_time.time.Imports._
import org.specs2.mutable._
import org.specs2.time.NoTimeConversions
import Implicits._

class ImplicitsSpec extends Specification with NoTimeConversions {
  "An ApsoTimeLocalDate" should {
    "support range creation" in {
      val startDate = "2014-01-01".toLocalDate
      val endDate = "2014-01-03".toLocalDate

      (startDate to endDate) === IndexedSeq(
        "2014-01-01".toLocalDate, "2014-01-02".toLocalDate, "2014-01-03".toLocalDate)

      (startDate until endDate) === IndexedSeq(
        "2014-01-01".toLocalDate, "2014-01-02".toLocalDate)

      (startDate until "2014-01-06".toLocalDate by 2.days) === IndexedSeq(
        "2014-01-01".toLocalDate, "2014-01-03".toLocalDate, "2014-01-05".toLocalDate)
    }

    "support conversion to UTC DateTime" in {
      val utcDateTime = "2014-01-01".toLocalDate.utcDateTime

      utcDateTime === new DateTime(2014, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC)
    }

    "support conversion to DateTime at the end of day" in {
      val dateTime = "2014-01-01".toLocalDate.toDateTimeAtEndOfDay

      dateTime === new DateTime(2014, 1, 1, 23, 59, 59, 999)
    }
  }
}