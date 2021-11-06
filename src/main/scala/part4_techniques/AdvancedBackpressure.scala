package part4_techniques

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import java.util.Date
import scala.concurrent.duration.DurationInt

object AdvancedBackpressure extends App {
  implicit val system = ActorSystem("AdvancedBackpressure")
  implicit val materializer = ActorMaterializer()

  // control backpressure
  val controllerFlow = Flow[Int].map(_ * 2).buffer(10, OverflowStrategy.dropHead)

  case class PagerEvent(description: String, date: Date, nInstances: Int = 1)
  case class Notification(email: String, pagerEvent: PagerEvent)

  val events = List(
    PagerEvent("service discovery failed", new Date),
    PagerEvent("illegal elements in the datapipeline", new Date),
    PagerEvent("number of HTTP 500 spiked", new Date),
    PagerEvent("Service stopped responding", new Date)
  )

  val eventSource = Source(events)

  val onCallEngineer = "daniel@rockthejvm.com" // a fast service for fetching oncall emails

  def sendEmail(notification: Notification) = {
    println(s"Dear ${notification.email}, you have an event ${notification.pagerEvent}") // this would actually send an email
  }

  val notificationSink = Flow[PagerEvent].map(event => Notification(onCallEngineer, event)).async
    .to(Sink.foreach[Notification](sendEmailSlow))

  // standard
  eventSource.to(notificationSink)
//    .run()

  // un-back-pressurable source
  def sendEmailSlow(notification: Notification) = {
    Thread.sleep(1000)
    println(s"Dear ${notification.email}, you have an event ${notification.pagerEvent}") // this would actually send an email
  }

  val aggregateNotificationFlow = Flow[PagerEvent]
    .conflate((event1, event2) => {
      val nInstances = event1.nInstances + event2.nInstances
      PagerEvent(s"you gave $nInstances events that require your attention", new Date, nInstances)
    })
    .map(resultingEvent => Notification(onCallEngineer, resultingEvent))

  eventSource.via(aggregateNotificationFlow).async
    .to(Sink.foreach[Notification](sendEmailSlow))
//    .run()
  // alternative to backpressure

  /*
  * Slow producers: extrapolate/expand
  * */

  val slowCounter = Source(Stream.from(1)).throttle(1000, 1 second)
  val hungrySink = Sink.foreach[Int](println)

  val extrapolator = Flow[Int].extrapolate(element => Iterator.continually(element))

  val expand = Flow[Int].expand(element => Iterator.from(element))

  slowCounter.via(extrapolator).to(hungrySink)
//    .run()
}
