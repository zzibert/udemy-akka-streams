package part4_techniques

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorSystem, Cancellable}
import akka.stream.scaladsl.GraphDSL.Implicits.{FanInOps, SourceArrow, fanOut2flow}
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.stream.scaladsl.{Balance, Broadcast, BroadcastHub, Concat, Flow, GraphDSL, Keep, Merge, MergeHub, MergePreferred, PartitionHub, RunnableGraph, Sink, Source, Tcp, Zip, ZipWith}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler, TimerGraphStageLogic}
import akka.stream.{ActorMaterializer, Attributes, ClosedShape, CompletionStrategy, DelayOverflowStrategy, FanInShape, FlowShape, Graph, Inlet, KillSwitches, Materializer, Outlet, OverflowStrategy, Shape, SourceShape, UniformFanInShape, UniqueKillSwitch}
import akka.util.ByteString
import java.util.concurrent.ThreadLocalRandom
import org.scalatest.Matchers.{convertToAnyShouldWrapper, equal}
import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object FaultTolerance extends App {
  val system = ActorSystem()

  implicit val mat = Materializer(system)

  implicit val ec: ExecutionContext = ExecutionContext.global

  // each time an event is pushed through it will trigger a period of silence
  class TimedGate[A](silencePeriod: FiniteDuration) extends GraphStage[FlowShape[A, A]] {

    val in = Inlet[A]("TimedGate.in")
    val out = Outlet[A]("TimedGate.out")

    val shape = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new TimerGraphStageLogic(shape) {

        var open = false

        setHandler(in, new InHandler {
          override def onPush(): Unit = {
            val elem = grab(in)
            if (open) pull(in)
            else {
              push(out, elem)
              open = true
              scheduleOnce(None, silencePeriod)
            }
          }
        })
        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            pull(in)
          }
        })

        override protected def onTimer(timerKey: Any): Unit = {
          open = false
        }
      }
  }

  val sourceTick = Source.tick(50.millis, 50.millis, "Tick")

  val sourceNumber = Source.fromIterator(() => Iterator.from(1))

  val sourceCombined = sourceTick.zipWith(sourceNumber) {(tick, number) =>
    s"$tick : $number"
  }

  val timerFlow = Flow.fromGraph(new TimedGate[String](200.millis))

  sourceCombined.via(timerFlow).runForeach(println) // 1, 5, 9, 13

}
