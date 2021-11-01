package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Sink, Source, ZipWith}

object GraphCycles extends App {
  implicit val system = ActorSystem("GraphCycles")
  implicit val materializer = ActorMaterializer()

  val accelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val incrementerShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating $x")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementerShape
                   mergeShape <~ incrementerShape

    ClosedShape
  }

//  RunnableGraph.fromGraph(accelerator).run()
  // graph cycle deadlock

  /*
  * Solution 1: Merge preferred
  * */

  val actualAccelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(MergePreferred[Int](1))
    val incrementerShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating $x")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementerShape
    mergeShape.preferred <~ incrementerShape

    ClosedShape
  }

  RunnableGraph.fromGraph(actualAccelerator)
//    .run()

  val bufferedRepeater = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val repeaterShape = builder.add(Flow[Int].buffer(size = 10, overflowStrategy = OverflowStrategy.dropHead).map { x =>
      println(s"Accelerating $x")
      Thread.sleep(200)
      x
    })

    sourceShape ~> mergeShape ~> repeaterShape
    mergeShape <~ repeaterShape

    ClosedShape
  }

//  RunnableGraph.fromGraph(bufferedRepeater).run()

  /*
  * Cycles risk deadlocking
  * - add bounds to the number of elements
  * boundedness vs liveness
  * */

  /*
  * Fan-in shape
  * - two inputs will be fed with exactly one number (1 and 1)
  * - out will emit an INFINITE FIBONACCI SEQUENCE based off those 2 numbers
  * 1, 1, 2, 3, 5, 8, 13, 21, ...
  * Hint: ZipWith, Cycles, MergePreferred
  * */

  val fibonacci = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val source1 = Source(List(1))
    val source2 = Source(List(1))

    val mergeShape1 = builder.add(MergePreferred[Int](1))
    val mergeShape2 = builder.add(MergePreferred[Int](1))
    val zipWithShape = builder.add(ZipWith[Int, Int, (Int, Int)]((first, second) => {
      Thread.sleep(1000)
      (second, first+second)
    }))
    val broadcastShape1 = builder.add(Broadcast[(Int, Int)](2))
    val broadcastShape2 = builder.add(Broadcast[Int](2))
    val filterFirstShape = builder.add(Flow[(Int, Int)].map(tuple => tuple._1))
    val filterSecondShape = builder.add(Flow[(Int, Int)].map(tuple => tuple._2))

    val sinkShape = builder.add(Sink.foreach[Int](println))

    source1 ~> mergeShape1.in(0) ; mergeShape1.out ~> zipWithShape.in0

    source2 ~> mergeShape2.in(0) ; mergeShape2 ~> zipWithShape.in1

    zipWithShape.out ~> broadcastShape1

    broadcastShape1.out(0) ~> filterFirstShape

    broadcastShape1.out(1) ~> filterSecondShape

    filterFirstShape ~> broadcastShape2

    broadcastShape2.out(0) ~> sinkShape

    broadcastShape2.out(1) ~> mergeShape1.preferred

    filterSecondShape ~> mergeShape2.preferred

    ClosedShape
  }

  RunnableGraph.fromGraph(fibonacci).run()

}
