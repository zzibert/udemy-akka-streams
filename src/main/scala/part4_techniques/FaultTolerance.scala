package part4_techniques

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorSystem, Cancellable}
import akka.stream.scaladsl.GraphDSL.Implicits.{FanInOps, SourceArrow, fanOut2flow}
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.stream.scaladsl.{Balance, Broadcast, BroadcastHub, Concat, Flow, GraphDSL, Keep, Merge, MergeHub, MergePreferred, PartitionHub, RunnableGraph, Sink, Source, Tcp, Zip, ZipWith}
import akka.stream.stage.{GraphStageLogic, OutHandler}
import akka.stream.{ActorMaterializer, Attributes, ClosedShape, CompletionStrategy, DelayOverflowStrategy, FanInShape, FlowShape, Graph, Inlet, KillSwitches, Materializer, Outlet, OverflowStrategy, Shape, SourceShape, UniformFanInShape, UniqueKillSwitch}
import akka.util.ByteString
import java.util.concurrent.ThreadLocalRandom
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

  import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler, StageLogging}

  final class RandomLettersSource extends GraphStage[SourceShape[String]] {
    val out = Outlet[String]("RandomLettersSource.out")
    override val shape: SourceShape[String] = SourceShape(out)

    override def createLogic(inheritedAttributes: Attributes) =
      new GraphStageLogic(shape) with StageLogging {
        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            val c = nextChar() // ASCII lower case letters

            // `log` is obtained from materializer automatically (via StageLogging)
            log.debug("Randomly generated: [{}]", c)

            push(out, c.toString)
          }
        })
      }

    def nextChar(): Char =
      ThreadLocalRandom.current().nextInt('a', 'z'.toInt + 1).toChar
  }

  val randomLettersSource = Source.fromGraph(new RandomLettersSource)

  randomLettersSource.take(10).runForeach(println)

}
