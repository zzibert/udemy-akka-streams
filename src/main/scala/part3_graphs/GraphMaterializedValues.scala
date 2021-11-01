package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, FlowShape, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}
import scala.concurrent.{Future, blocking}
import scala.util.{Failure, Success}

object GraphMaterializedValues extends App {
  implicit val system = ActorSystem("GraphMaterializedValues")
  implicit val materializer = ActorMaterializer()

  val wordSource = Source(List("Akka", "is", "awesome", "rock", "the", "jvm"))

  val printer = Sink.foreach[String](println)

  val counter1 = Sink.fold[Int, String](0)((count, _) => count + 1)

  /*
  * A composite component (sink)
  * -prints out all strings which are lowercase
  * - counts the strings that are short (5 > chars)
  * */

  val complexWordSink = Sink.fromGraph(
    GraphDSL.create(printer, counter1)((printerMatValue, counterMatValue) => (counterMatValue)) { implicit builder => (printerShape, counterShape) =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[String](2))
      val lowerCaseFilter = builder.add(Flow[String].filter(!_.exists(_.isUpper)))
      val shortFilter = builder.add(Flow[String].filter(_.length < 5))

      broadcast.out(0) ~> lowerCaseFilter ~> printerShape
      broadcast.out(1) ~> shortFilter ~> counterShape

      SinkShape(broadcast.in)
    }
  )

  val shortStringCountFuture = wordSource.toMat(complexWordSink)(Keep.right)
//    .run()

  import scala.concurrent.ExecutionContext.Implicits.global

//  shortStringCountFuture.onComplete {
//    case Success(value) => println(value)
//    case Failure(exception) => println(exception)
//  }

  /*
  * Exercice
  * */
  def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val counter = Sink.fold[Int, B](0)((count, _) => count + 1)
    Flow.fromGraph(
      GraphDSL.create(counter) { implicit builder => (counterShape) =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[B](2))
        val buildFlow = builder.add(flow)

        buildFlow ~> broadcast

        broadcast.out(0) ~> counterShape

        FlowShape(in = buildFlow.in, out = broadcast.out(1))
      }
    )
  }
  // use broadcast and sink.fold

  val flow = Flow[Int].map(_ * 10)
  val source = Source(1 to 100)
  val flowCounter = enhanceFlow(flow)

  val flowCount = source.viaMat(flowCounter)(Keep.right).to(Sink.foreach(println)).run()

  flowCount .onComplete {
    case Success(value) => println(s"the number of elements that went through flow is: $value")
    case Failure(exception) => println(exception)
  }


}
