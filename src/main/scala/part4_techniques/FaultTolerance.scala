package part4_techniques


import akka.NotUsed
import akka.actor.{Actor, ActorSystem, Cancellable, Props}
import akka.pattern.ask
import akka.stream.scaladsl.{Balance, Broadcast, BroadcastHub, Concat, Flow, GraphDSL, Keep, Merge, MergeHub, MergePreferred, PartitionHub, RunnableGraph, Sink, Source, Tcp, Zip, ZipWith}
import akka.stream.{ActorMaterializer, Attributes, ClosedShape, CompletionStrategy, DelayOverflowStrategy, FanInShape, FlowShape, Graph, Inlet, KillSwitches, Materializer, Outlet, OverflowStrategy, Shape, SourceShape, UniformFanInShape, UniqueKillSwitch}
import akka.util.Timeout
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, Promise}

object FaultTolerance extends App {
  val system = ActorSystem()

  implicit val mat = Materializer(system)

  implicit val ec: ExecutionContext = ExecutionContext.global

  implicit val askTimeout: Timeout = 5.seconds
  val words: Source[String, NotUsed] =
    Source(List("hello", "hi", "ho", "hu", "ha", "trolo", "lolo"))

  class Translator extends Actor {
    def receive = {
      case word: String =>
        // ... process message
        val reply = word.toUpperCase
        sender() ! reply // reply to the ask
    }
  }

  val ref = system.actorOf(Props(new Translator))

  words
    .mapAsyncUnordered(2)(ref ? _)
    .runForeach(println)

}
