package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FanOutShape2, FlowShape, SourceShape, UniformFanInShape, UniformFanOutShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}
import java.util.Date

object MoreOpenGraphs extends App {
  implicit val system = ActorSystem("MoreOpenGraphs")
  implicit val materializer = ActorMaterializer()

  /*
  * Example Max3 Operator
  * - 3 inputs of type Int
  * - the maximum of the 3
  * */

  val source1 = Source(1 to 100)
  val source2 = Source((1 to 100).reverse).map(_ * 2)
  val source3 = Source(1 to 100).map(_ * 5)


  val max3StaticGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val firstZipWith = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))
    val secondZipWith = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))

    firstZipWith.out ~> secondZipWith.in0

    UniformFanInShape(secondZipWith.out, firstZipWith.in0, firstZipWith.in1, secondZipWith.in1)
  } // static graph

  val max3Graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val max3Shape = builder.add(max3StaticGraph)

      source1 ~> max3Shape
      source2 ~> max3Shape
      source3 ~> max3Shape

      max3Shape ~> Sink.foreach(println)

      ClosedShape
    }
  )

//  max3Graph.run()
  
  // Non-Uniform fan out shape
  /*
  * Bank transactions
  * - more than 10 000 $
  * streams component for transactions
  * - unmodified let the transaction go through -output1
  * - gives only back suspicious transactions ids
  * */
  
  case class Transaction(id: String, source: String, recepient: String, amount: Int, date: Date)
  
  val transactionsSource = Source(List(
    Transaction(id = "37298", source = "Paul", recepient = "andrew", amount = 100, date = new Date),
    Transaction(id = "38298", source = "Daniel", recepient = "Jim", amount = 100000, date = new Date),
    Transaction(id = "47298", source = "Jim", recepient = "Alice", amount = 7000, date = new Date)
  ))

  val bankProcessor = Sink.foreach[Transaction](println)
  val suspiciousAnalysisService = Sink.foreach[String](id => println(s"Suspicious transaction id: $id"))

  val suspiciousTransactionStaticGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    // step 2 - define shapes
    val broadcast = builder.add(Broadcast[Transaction](2))
    val suspiciousTransactionFilter = builder.add(Flow[Transaction].filter(_.amount > 10000))
    val extractIdFlow = builder.add(Flow[Transaction].map[String](_.id))

    broadcast.out(1) ~> suspiciousTransactionFilter ~> extractIdFlow

    new FanOutShape2[Transaction, Transaction, String](broadcast.in, broadcast.out(0), extractIdFlow.out)
  } // static graph

  val transactionRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val transactionGraph = builder.add(suspiciousTransactionStaticGraph)

      transactionsSource ~> transactionGraph.in

      transactionGraph.out0 ~> bankProcessor
      transactionGraph.out1 ~> suspiciousAnalysisService

      ClosedShape
    }
  )

  transactionRunnableGraph.run()


}
