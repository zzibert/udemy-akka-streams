package part4_techniques

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorSystem, Cancellable}
import akka.stream.scaladsl.GraphDSL.Implicits.{FanInOps, SourceArrow, fanOut2flow}
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.stream.scaladsl.{Balance, Broadcast, BroadcastHub, Concat, Flow, GraphDSL, Keep, Merge, MergeHub, MergePreferred, PartitionHub, RunnableGraph, Sink, Source, Tcp, Zip, ZipWith}
import akka.stream.{ActorMaterializer, Attributes, ClosedShape, CompletionStrategy, DelayOverflowStrategy, FanInShape, FlowShape, Graph, Inlet, KillSwitches, Materializer, Outlet, OverflowStrategy, Shape, SourceShape, UniformFanInShape, UniqueKillSwitch}
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


  // A simple producer that publishes a new "message-" every second
  val producer = Source.tick(1.second, 1.second, "message").zipWith(Source(1 to 100))((a, b) => s"$a-$b")

  // New instance of the partitioner function and its state is created
  // for each materialization of the PartitionHub.
  def roundRobin(): (PartitionHub.ConsumerInfo, String) => Long = {
    var i = -1L

    (info, elem) => {
      i += 1
      info.consumerIdByIdx((i % info.size).toInt)
    }
  }

  // Attach a PartitionHub Sink to the producer. This will materialize to a
  // corresponding Source.
  // (We need to use toMat and Keep.right since by default the materialized
  // value to the left is used)
  val runnableGraph: RunnableGraph[Source[String, NotUsed]] =
  producer.toMat(PartitionHub.statefulSink(() => roundRobin(), startAfterNrOfConsumers = 2, bufferSize = 256))(
    Keep.right)

  // By running/materializing the producer, we get back a Source, which
  // gives us access to the elements published by the producer.
  val fromProducer: Source[String, NotUsed] = runnableGraph.run()

  // Print out messages from the producer in two independent consumers
  fromProducer.runForeach(msg => println("consumer1: " + msg))
  fromProducer.runForeach(msg => println("consumer2: " + msg))

}
