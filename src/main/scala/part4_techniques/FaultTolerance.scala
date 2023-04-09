package part4_techniques

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorSystem, Cancellable}
import akka.stream.javadsl.ZipWith
import akka.stream.{ActorMaterializer, ClosedShape, CompletionStrategy, Materializer, OverflowStrategy, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source}
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object FaultTolerance extends App {
  val system = ActorSystem()

  implicit val mat = Materializer(system)

  implicit val ec: ExecutionContext = ExecutionContext.global

  val pickMaxOfThree = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val zip1 = b.add(ZipWith.create[Int, Int, Int](math.max(_, _)))
    val zip2 = b.add(ZipWith.create[Int, Int, Int](math.max(_, _)))
    zip1.out ~> zip2.in0

    UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
  }

  val resultSink = Sink.head[Int]

  val g = RunnableGraph.fromGraph(GraphDSL.createGraph(resultSink) { implicit b =>
    sink =>
      import GraphDSL.Implicits._

      // importing the partial graph will return its shape (inlets & outlets)
      val pm3 = b.add(pickMaxOfThree)

      Source.single(1) ~> pm3.in(0)
      Source.single(2) ~> pm3.in(1)
      Source.single(3) ~> pm3.in(2)
      pm3.out ~> sink.in
      ClosedShape
  })

  val max: Future[Int] = g.run()

  max.onComplete(println(_))

}
