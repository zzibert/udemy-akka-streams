package part4_techniques

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorSystem, Cancellable}
import akka.stream.scaladsl.GraphDSL.Implicits.{FanInOps, SourceArrow, fanOut2flow}
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.stream.scaladsl.{Balance, Broadcast, Concat, Flow, GraphDSL, Keep, Merge, MergeHub, MergePreferred, RunnableGraph, Sink, Source, Tcp, Zip, ZipWith}
import akka.stream.{ActorMaterializer, Attributes, ClosedShape, CompletionStrategy, DelayOverflowStrategy, FanInShape, FlowShape, Graph, Inlet, KillSwitches, Materializer, Outlet, OverflowStrategy, Shape, SourceShape, UniformFanInShape}
import akka.util.ByteString
import org.scalatest.Matchers.{convertToAnyShouldWrapper, equal}
import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object FaultTolerance extends App {
  val system = ActorSystem()

  implicit val mat = Materializer(system)

  implicit val ec: ExecutionContext = ExecutionContext.global


  // A simple consumer that will print to the console for now
  val consumer = Sink.foreach(println)

  // Attach a MergeHub Source to the consumer. This will materialize to a
  // corresponding Sink.
  val runnableGraph: RunnableGraph[Sink[String, NotUsed]] =
  MergeHub.source[String](perProducerBufferSize = 16).to(consumer)

  // By running/materializing the consumer we get back a Sink, and hence
  // now have access to feed elements into it. This Sink can be materialized
  // any number of times, and every element that enters the Sink will
  // be consumed by our consumer.
  val toConsumer: Sink[String, NotUsed] = runnableGraph.run()

  // Feeding two independent sources into the hub.
  Source.tick(0.millis, 500.millis, "ihihihih").runWith(toConsumer)
  Source.tick(0.millis, 300.millis, "Trololo").runWith(toConsumer)

}
