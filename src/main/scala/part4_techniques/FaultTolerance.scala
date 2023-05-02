package part4_techniques


import akka.actor.{Actor, ActorSystem, Cancellable}
import akka.stream.scaladsl.{Balance, Broadcast, BroadcastHub, Concat, Flow, GraphDSL, Keep, Merge, MergeHub, MergePreferred, PartitionHub, RunnableGraph, Sink, Source, Tcp, Zip, ZipWith}
import akka.stream.{ActorMaterializer, Attributes, ClosedShape, CompletionStrategy, DelayOverflowStrategy, FanInShape, FlowShape, Graph, Inlet, KillSwitches, Materializer, Outlet, OverflowStrategy, Shape, SourceShape, UniformFanInShape, UniqueKillSwitch}
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{Await, ExecutionContext, Future, Promise}

object FaultTolerance extends App {
  val system = ActorSystem()

  implicit val mat = Materializer(system)

  implicit val ec: ExecutionContext = ExecutionContext.global

  class SometimesSlowService(implicit ec: ExecutionContext) {

    private val runningCount = new AtomicInteger

    def convert(s: String): Future[String] = {
      println(s"running: $s (${runningCount.incrementAndGet()})")
      Future {
        if (s.nonEmpty && s.head.isLower)
          Thread.sleep(500)
        else
          Thread.sleep(20)
        println(s"completed: $s (${runningCount.decrementAndGet()})")
        s.toUpperCase
      }
    }
  }


  val service = new SometimesSlowService

  Source(List("a", "B", "C", "D", "e", "F", "g", "H", "i", "J"))
    .map(elem => {
      println(s"before: $elem"); elem
    })
    .mapAsync(4)(service.convert)
    .to(Sink.foreach(elem => println(s"after: $elem")))
    .withAttributes(Attributes.inputBuffer(initial = 4, max = 4))
    .run()

  // A, B, C, D, E, F, G, ...

}
