package eu.shiftforward.apso.io

import java.io.InputStream

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import eu.shiftforward.apso.Logging

/**
 * A InputStream that wraps another InputStream, retrying failed reads. This is useful for input streams that can have
 * transient failures (eg HTTP input streams).
 *
 * @param streamBuilder function that returns a new stream.
 * @param maxRetries maximum number of times to retry a read
 * @param backoff optional duration to wait between retries
 */
class InsistentInputStream(streamBuilder: () => InputStream, maxRetries: Int = 10, backoff: Option[FiniteDuration] = None) extends InputStream with Logging {

  private[this] var innerStream: InputStream = streamBuilder()
  private[this] var currPos: Long = 0

  @tailrec
  private[this] def retryStreamCreation(remainingTries: Int): Int = {
    Try(innerStream.close()) // continue even if the close operation fails
    Try {
      backoff.foreach(d => Thread.sleep(d.toMillis))
      innerStream = streamBuilder()
      innerStream.skip(currPos)
    } match {
      case Success(n) =>
        remainingTries
      case Failure(t) =>
        if (remainingTries <= 0) throw t
        else retryStreamCreation(remainingTries - 1)
    }
  }

  @tailrec
  private[this] def readRetries(remainingTries: Int, f: => Int): Int = {
    Try(f) match {
      case Success(n) =>
        n
      case Failure(t) =>
        if (remainingTries <= 0) throw t
        else {
          log.warn("Failed to read from stream: {}", t)
          val nextRemainingTries = retryStreamCreation(remainingTries - 1)
          readRetries(nextRemainingTries, f)
        }
    }
  }

  def read(): Int = {
    val nextByte = readRetries(maxRetries, innerStream.read())
    if (nextByte > 0) currPos += 1
    nextByte
  }

  override def read(b: Array[Byte]): Int = {
    val bytesRead = readRetries(maxRetries, innerStream.read(b))
    if (bytesRead > 0) currPos += bytesRead
    bytesRead
  }

  override def read(b: Array[Byte], off: Int, len: Int): Int = {
    val bytesRead = readRetries(maxRetries, innerStream.read(b, off, len))
    if (bytesRead > 0) currPos += bytesRead
    bytesRead
  }

  override def available(): Int = innerStream.available()
  override def close() = innerStream.close()
  override def skip(n: Long) = {
    val skipped = innerStream.skip(n)
    currPos += skipped
    skipped
  }
}
