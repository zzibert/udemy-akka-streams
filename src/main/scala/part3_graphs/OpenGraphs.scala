package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, RunnableGraph, Sink, Source}

object OpenGraphs extends App {
  implicit val system = ActorSystem("OpenGraphs")

  implicit val materializer = ActorMaterializer()

  /*
  * A composite source that concatenates 2 sources
  * - emits ALL the elements from the first source
  * - then ALL the elements from the second
  * */

  val firstSource = Source(1 to 10)
  val secondSource = Source(42 to 1000)

  val sourceGraph = Source.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // step 2 - declaring componenets
      val concat = builder.add(Concat[Int](2))

      // step 3 - tying them togther
      firstSource ~> concat
      secondSource ~> concat

      //step 4
      SourceShape(concat.out)
    }
  )

//  sourceGraph.to(Sink.foreach(println)).run()

  /*
  * Complex sink
  *
  * */
  val sink1 = Sink.foreach[Int](x => println(s"Meaningful thing 1: $x"))
  val sink2 = Sink.foreach[Int](x => println(s"Meaningful thing 2: $x"))

  val sinkGraph = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))

      broadcast ~> sink1
      broadcast ~> sink2

      SinkShape(broadcast.in)
    }
  )

  /*
  * Challange - complex flow
  * Write your own flow thats composed of two other flows
  * - adds one to a number
  * - one that does number * 10
  */

  val incrementer = Flow[Int].map(_ + 1)
  val multiplier = Flow[Int].map(_ * 10)


  val flowGraph = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // everything operates on SHAPES
      val incrementerShape = builder.add(incrementer)
      val multiplierShape = builder.add(multiplier)

      incrementerShape ~> multiplierShape

      FlowShape(incrementerShape.in, multiplierShape.out)
    } // static graph
  ) // component

//  firstSource.via(flowGraph).to(sink1).run()

  val f = Flow.fromSinkAndSourceCoupled(Sink.foreach[String](println), Source(1 to 10))
}
