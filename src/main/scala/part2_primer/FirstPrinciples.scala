package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.concurrent.Future

object FirstPrinciples extends App {
  implicit val system = ActorSystem("FirstPrinciples")

  implicit val materializer = ActorMaterializer()

  // Source
  val source = Source(1 to 10)

  // Sink
  val sink = Sink.foreach[Int](println)

  val graph = source.to(sink)

//  graph.run()

  // flows transforms elements
  val flow = Flow[Int].map(_ + 1)

  val sourceWithFlow = source.via(flow)

  val flowWithSink = flow.to(sink)

//  sourceWithFlow.to(sink).run()

//  source.via(flow).via(flow).to(sink).run()

  // nulls are NOT allowed
  // use options instead
//  val illegalSource = Source.single[String](null)

//  illegalSource.to(Sink.foreach(println)).run()

  val finiteSource = Source.single(1)
  val anotherFiniteSource = Source(List(1, 2, 3))

  val emptySource = Source.empty[Int]

  // do not confuse akka streams with collection stream
  val infiniteSource = Source(Stream.from(1))

  import scala.concurrent.ExecutionContext.Implicits.global

  val futureSource = Source.fromFuture(Future(42))

  // sink
  val theMostBoringSink = Sink.ignore

  val forEachSink = Sink.foreach[String](println)

  val headSink = Sink.head[Int] // retries head and then closes the stream

  val foldSink = Sink.fold[Int, Int](0)(_ + _)

  // flows - usually mapped to collection operators
  val mapFlow = Flow[Int].map(_ * 2)
  val takeFlow = Flow[Int].take(5)

  // NOT have flatMap

  // source -> flow -> flow -> ... -> sink

//  val doubleFlowGraph = source.via(mapFlow).via(takeFlow).to(sink).run()

  val mapSource = Source(1 to 10).map(_ * 2)

  // run streams directly
  //  mapSource.runForeach(println) // mapSource.to(Sink.foreach[Int](println))

  // OPERATORS = components

  /*
  * create a stream that takes names of persons,
  * then keep the first two with length greater than 5 characters
  * */

  val names = List("james", "giannis", "mike", "bob", "alfred", "abraham", "william")

  Source(names).filter(_.length > 5).take(2).runForeach(println)


}