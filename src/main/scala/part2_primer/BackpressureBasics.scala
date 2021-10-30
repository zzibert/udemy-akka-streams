package part2_primer

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.concurrent.duration.DurationInt

object BackpressureBasics extends App {
  implicit val system = ActorSystem("BackpressureBasics")

  implicit val materializer = ActorMaterializer()

  val fastSource = Source((1 to 1000).map {x =>
    println(s"Source $x")
    x
  })

  val slowSink = Sink.foreach[Int]{ x =>
  Thread.sleep(500)
    println(s"Sink $x")
  }

  val normalSink = Sink.foreach[Int](println)

  // NO BACKPRESSURE
  fastSource
    .to(slowSink)
//    .run()

  // BACKPRESSURE
  fastSource
    .async
    .to(slowSink)
//    .run()

  val simpleFlow = Flow[Int].map { x =>
    println(s"Incoming: $x")
    x + 1
  }

  fastSource.async
    .via(simpleFlow).async
    .to(slowSink)
//    .to(normalSink)
//    .run()

  /*
  * - reactions to backpressure (in order):
  * - try to slow down if possible
  * - buffer elements until there is more demand
  * - drop down elements from the buffer if it overlflows
  * - tear down the whole stream (failure)
  * */

  val bufferedFlow = simpleFlow.buffer(10, overflowStrategy = OverflowStrategy.dropBuffer)

  fastSource.async
    .via(bufferedFlow).async
    .to(slowSink)
//    .run()

  /* 1-16: nobody is backpressured
  * 17 - 26 - flow buffers, flow will start droping at the next element
  * 26 - 1000 flow will always drop the oldest element
  * 991 - 1001 992 - 1001
  * */

  // throttling
  fastSource.throttle(16, 1 second).async
    .via(bufferedFlow).async
    .runWith(slowSink)
}
