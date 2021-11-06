package part4_techniques

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.Timeout
import java.util.Date
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object IntegratingWithExternalServices extends App {
  implicit val system = ActorSystem("IntegratingWithExternalServices")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
//  implicit val dispatcher = system.dispatchers.lookup("dedicated-dispatcher")

  def genericExtService[A, B](element: A): Future[B] = ???

  // simplified PagerDuty

  case class PagerEvent(application: String, description: String, date: Date)


  val eventSource = Source(List(
    PagerEvent("AkkaInfra", "Infrastructure broke", new Date),
    PagerEvent("FastDatapipeline", "Illegal elements in the data pipeline", new Date),
    PagerEvent("AkkaInfra", "A service stopped responding", new Date),
    PagerEvent("SuperFrontend", "A button doesnt work", new Date)
  ))

  class PagerActor extends Actor with ActorLogging {
    private val engineers = List("Daniel", "John", "lady Gaga")
    private val emails = Map(
      "Daniel" -> "daniel@rockthejvm.com",
      "John" -> "john@rockthehvm.com",
      "lady Gaga" -> "ladygaga@rockthejvm.com"
    )

    override def receive: Receive = {
      case pagerEvent: PagerEvent => {
        sender() ! processEvent(pagerEvent)
      }
    }

    def processEvent(pagerEvent: PagerEvent): String = {
      val engineerIndex = pagerEvent.date.toInstant.getEpochSecond / (24 * 3600) % engineers.length
      val engineer = engineers(engineerIndex.toInt)
      val email = emails(engineer)

      // page the engineer
      println(s"Sending engineer $email a high priority notification: $pagerEvent")
      Thread.sleep(1000)

      email
    }
  }

  val infraEvents = eventSource.filter(_.application == "AkkaInfra")
//  val pagedEngineerEmail = infraEvents.mapAsync(parallelism = 1)(event => PagerService.processEvent(event))
  // guarantees the relative order of elements

  val pagedEmailsSink = Sink.foreach[String](email => println(s"Succesfully sent notification to $email"))

//  pagedEngineerEmail.to(pagedEmailsSink).run()

  val pagerActor = system.actorOf(Props[PagerActor], "pagerActor")

  implicit val timeout = Timeout(3 seconds)

  infraEvents.mapAsync(parallelism = 4)(event => (pagerActor ? event).mapTo[String]).to(pagedEmailsSink).run()

  // do not confuse mapAsync with async (ASYNC BOUNDARY)
}
