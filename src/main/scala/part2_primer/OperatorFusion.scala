package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object OperatorFusion extends App {

  implicit val system = ActorSystem("operatorFusion")
  implicit val materializer = ActorMaterializer()

  val simpleSource = Source(1 to 1000)
  val simpleFlow = Flow[Int].map {x =>
    Thread.sleep(1000)
    x + 1
  }
  val simpleFlow2 = Flow[Int].map {x =>
    Thread.sleep(1000)
    x * 10
  }
  val simpleSink1 = Sink.foreach[Int](value => println(s"sink1: $value"))

  val simpleSink2 = Sink.foreach[Int](value => println(s"sink2: $value"))

  // this runs on the SAME ACTOR
//  simpleSource.via(simpleFlow).via(simpleFlow2).to(simpleSink1).run()
  // OPERATOR FUSION

  // complex operator flows
  val complexFlow = Flow[Int].map {x =>
    Thread.sleep(1000)
    x + 1
  }

  // ASYNC BOUNDARIES
  simpleSource.via(simpleFlow).async // runs on one actor
    .via(simpleFlow2).async // runs on seperate actor
    .to(simpleSink2) // runs on third actor
//    .run()

  // ORDERING GUARANTEES
  Source(1 to 3)
    .map(element => {println(s"Flow A: $element"); element }).async
    .map(element => {println(s"Flow B: $element"); element }).async
    .map(element => {println(s"Flow C: $element"); element }).async
    .map(element => {println(s"Flow D: $element"); element }).async
    .runWith(Sink.ignore)
}
