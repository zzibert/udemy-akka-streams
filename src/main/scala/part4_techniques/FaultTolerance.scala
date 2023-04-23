package part4_techniques

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorSystem, Cancellable}
import akka.stream.scaladsl.GraphDSL.Implicits.{FanInOps, SourceArrow, fanOut2flow}
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.stream.scaladsl.{Balance, Broadcast, BroadcastHub, Concat, Flow, GraphDSL, Keep, Merge, MergeHub, MergePreferred, PartitionHub, RunnableGraph, Sink, Source, Tcp, Zip, ZipWith}
import akka.stream.stage.GraphStageLogic
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

  import akka.stream.SourceShape
  import akka.stream.stage.GraphStage

  class NumbersSource extends GraphStage[SourceShape[Int]] {
    // Define the (sole) output port of this stage
    val out: Outlet[Int] = Outlet("NumbersSource")
    // Define the shape of this stage, which is SourceShape with the port we defined above
    override val shape: SourceShape[Int] = SourceShape(out)

    // This is where the actual (possibly stateful) logic will live
    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = ???
  }

}
