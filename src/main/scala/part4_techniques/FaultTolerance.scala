package part4_techniques

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorSystem, Cancellable}
import akka.stream.scaladsl.GraphDSL.Implicits.{FanInOps, SourceArrow, fanOut2flow}
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.stream.scaladsl.{Balance, Broadcast, BroadcastHub, Concat, Flow, GraphDSL, Keep, Merge, MergeHub, MergePreferred, RunnableGraph, Sink, Source, Tcp, Zip, ZipWith}
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


  // Obtain a Sink and Source which will publish and receive from the "bus" respectively.
  val (sink, source) =
    MergeHub.source[String](perProducerBufferSize = 16).toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both).run()

  // Ensure that the Broadcast output is dropped if there are no listening parties.
  // If this dropping Sink is not attached, then the broadcast hub will not drop any
  // elements itself when there are no subscribers, backpressuring the producer instead.
//  source.runWith(Sink.ignore)

  // We create now a Flow that represents a publish-subscribe channel using the above
  // started stream as its "topic". We add two more features, external cancellation of
  // the registration and automatic cleanup for very slow subscribers.
  val busFlow: Flow[String, String, UniqueKillSwitch] =
  Flow
    .fromSinkAndSource(sink, source)
    .joinMat(KillSwitches.singleBidi[String, String])(Keep.right)
    .backpressureTimeout(3.seconds)

  val switch: UniqueKillSwitch =
    Source.repeat("Hello world!").viaMat(busFlow)(Keep.right).to(Sink.foreach(println)).run()

  // Shut down externally

  switch.shutdown()

}
