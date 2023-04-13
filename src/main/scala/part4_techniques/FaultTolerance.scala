package part4_techniques

import akka.{Done, NotUsed}
import akka.actor.{Actor, ActorSystem, Cancellable}
import akka.stream.javadsl.ZipWith
import akka.stream.{ActorMaterializer, ClosedShape, CompletionStrategy, FanInShape, FlowShape, Graph, Inlet, Materializer, Outlet, OverflowStrategy, Shape, SourceShape, UniformFanInShape}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Keep, Merge, MergePreferred, RunnableGraph, Sink, Source, Zip}
import org.scalatest.Matchers.{convertToAnyShouldWrapper, equal}
import scala.collection.immutable
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object FaultTolerance extends App {
  val system = ActorSystem()

  implicit val mat = Materializer(system)

  implicit val ec: ExecutionContext = ExecutionContext.global

  import FanInShape.{Init, Name}

  //  class PriorityWorkerPoolShape2[In, Out](_init: Init[Out] = Name("PriorityWorkerPool"))
  //    extends FanInShape[Out](_init) {
  //    protected override def construct(i: Init[Out]) = new PriorityWorkerPoolShape2(i)
  //
  //    val jobsIn = newInlet[In]("jobsIn")
  //    val priorityJobsIn = newInlet[In]("priorityJobsIn")
  //    // Outlet[Out] with name "out" is automatically created
  //  }

  // A shape represents the input and output ports of a reusable
  // processing module
  case class PriorityWorkerPoolShape[In, Out](jobsIn: Inlet[In], priorityJobsIn: Inlet[In], resultsOut: Outlet[Out])
    extends Shape {

    // It is important to provide the list of all input and output
    // ports with a stable order. Duplicates are not allowed.
    override val inlets: immutable.Seq[Inlet[_]] =
    jobsIn :: priorityJobsIn :: Nil
    override val outlets: immutable.Seq[Outlet[_]] =
      resultsOut :: Nil

    // A Shape must be able to create a copy of itself. Basically
    // it means a new instance with copies of the ports
    override def deepCopy() =
      PriorityWorkerPoolShape(jobsIn.carbonCopy(), priorityJobsIn.carbonCopy(), resultsOut.carbonCopy())

  }

  object PriorityWorkerPool {
    def apply[In, Out](
                        worker: Flow[In, Out, Any],
                        workerCount: Int): Graph[PriorityWorkerPoolShape[In, Out], NotUsed] = {

      GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._

        val priorityMerge = b.add(MergePreferred[In](1))
        val balance = b.add(Balance[In](workerCount))
        val resultsMerge = b.add(Merge[Out](workerCount))

        // After merging priority and ordinary jobs, we feed them to the balancer
        priorityMerge ~> balance

        // Wire up each of the outputs of the balancer to a worker flow
        // then merge them back
        for (i <- 0 until workerCount)
          balance.out(i) ~> worker ~> resultsMerge.in(i)

        // We now expose the input ports of the priorityMerge and the output
        // of the resultsMerge as our PriorityWorkerPool ports
        // -- all neatly wrapped in our domain specific Shape
        PriorityWorkerPoolShape(
          jobsIn = priorityMerge.in(0),
          priorityJobsIn = priorityMerge.preferred,
          resultsOut = resultsMerge.out)
      }

    }

  }

}
