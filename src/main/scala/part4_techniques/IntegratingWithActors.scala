package part4_techniques

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import scala.concurrent.duration.DurationInt

object IntegratingWithActors extends App {
  implicit val system = ActorSystem("IntegratingWithActors")
  implicit val materializer = ActorMaterializer()

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case s: String =>
        log.info(s"Just received a string: $s")
        sender() ! s"$s$s"
      case n: Int =>
        log.info(s"Just received a number: $n")
        sender() ! n * 2
      case _ =>
    }
  }

  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  val numberSource = Source(1 to 10)

  // actor as a flow

  implicit val timeout = Timeout(2 second)

  val actorBasedFlow = Flow[Int].ask[Int](parallelism = 4)(simpleActor)

  numberSource.via(actorBasedFlow).to(Sink.ignore)
//    .run()
  numberSource.ask[Int](parallelism = 4)(simpleActor).to(Sink.ignore)
//    .run() // equivalent

  // ACTOR AS A SOURCE
  /*
  *
  * */
  val actorPoweredSource = Source.actorRef[Int](bufferSize = 10, overflowStrategy = OverflowStrategy.dropHead)

  val materializedActorRef = actorPoweredSource.to(Sink.foreach[Int](n => println(s"Actor powered flow got number: $n")))
//    .run()

//  materializedActorRef ! 10
//  materializedActorRef ! 20
//  materializedActorRef ! 100
//
//  // terminating the stream
//  materializedActorRef ! akka.actor.Status.Success("Complete")

  // Actor as a destination / sink for messages
  /*
  * - an init message
  * - an ack message to confirm the reception of the message
  * - a complete message
  * - a function to generate a message in case the stream throws an exception
  * */

  case object StreamInit

  case object StreamAck

  case object StreamComplete

  case class StreamFail(exception: Throwable)

  class DestinationActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case StreamInit => {
        log.info("Stream Initialized")
        sender() ! StreamAck
      }

      case StreamComplete => {
        log.info("Stream Complete")
        context.stop(self)
      }

      case StreamFail(e) => {
        log.warning(s"Stream failed: $e")
      }

      case message => {
        log.info(s"Message $message has come to its final resting point")
        sender() ! StreamAck
      }
    }
  }

  val destinationActor = system.actorOf(Props[DestinationActor], "destinationActor")

  val actorPoweredSink = Sink.actorRefWithAck[Int](
    destinationActor,
    onInitMessage = StreamInit,
    onCompleteMessage = StreamComplete,
    ackMessage = StreamAck,
    onFailureMessage = throwable => StreamFail(throwable)
  )

  Source(1 to 10).to(actorPoweredSink)
//    .run()

//  Sink.actorRef() not recommend , unable to provide backpressure

}
