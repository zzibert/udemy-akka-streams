package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}
import scala.concurrent.duration.DurationInt

object GraphBasics extends App {
  implicit val system = ActorSystem("GraphBasics")
  implicit val materializer = ActorMaterializer()

  val input = Source(1 to 1000)

  // assuming two hard computation
  val incrementer = Flow[Int].map(_ + 1)
  val multiplier = Flow[Int].map(_ * 10)
  val output = Sink.foreach[(Int, Int)](println)
  val sink1 = Sink.fold[Int, Int](0)((elements, _) => {
    println(s"SINK 1: $elements")
    elements + 1
  })
  val sink2 = Sink.fold[Int, Int](0)((elements, _) => {
    println(s"SINK 2: $elements")
    elements + 1
  })

  // step 1 - setting up the fundamentals for the graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // mutable data structure
      import GraphDSL.Implicits._ // brings some nice operators into scope

      // step 2 - add the necessary components of this graph
      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator
      val zip = builder.add(Zip[Int, Int]) // fan-in operator

      // step 3 - tying up the components
      input ~> broadcast

      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~> output

      // step 4 - return a closed shape
      ClosedShape // freeze the builders shape

      // shape
    } // static graph
  ) // runnable graph

//  graph.run() // run the graph and materialize it

  /*
  * exercise 1 - feed a source into 2 sinks at the same time , hi
  * */

  val graph2 = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))

      // implicit port numbering
      input ~> broadcast ~> sink1
      broadcast ~> sink2

//      broadcast.out(0) ~> sink1
//      broadcast.out(1) ~> sink2

      ClosedShape
    }
  )

//  graph2.run()

  val fastSource = input.throttle(5, 1 second)
  val slowSource = input.throttle(2, 1 second)

  val graph3 = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[Int](2, false))
      val balance = builder.add(Balance[Int](2, true))

      fastSource ~> merge ~> balance ~> sink1
      slowSource ~> merge; balance ~> sink2

      ClosedShape
    }
  )

  graph3.run()
}
