package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

object FirstPrinciples extends App {

  implicit val system = ActorSystem("FirstPrinciples")
  implicit val materializer = ActorMaterializer()

  // sources

  // sink
  val sink = Sink.foreach[Int](println)

//  val graph = source.to(sink)

//  graph.run()

  // flows transform elements
  val flow = Flow[Int].map(x => x + 1)

//  val sourceWithFlow = source.via(flow)

  val flowWithSink = flow.to(sink)

//  sourceWithFlow.to(sink).run()

//  source.to(flowWithSink).run()

//  source.via(flow).to(sink).run()

  // nulls are not allowed
//  val illegalSource = Source.single[String](null)

//  illegalSource.to(Sink.foreach(println))

  val finiteSource = Source.single(1)

  val anotherFiniteSource = Source(List(1, 2, 3))

  val emptySource = Source.empty[Int]

  val infiniteSource = Source(Stream.from(1)) // do not confuse akka stream with a collection stream

  import scala.concurrent.ExecutionContext.Implicits.global

  val futureSource = Source.fromFuture(Future(42))

  // sinks
  val theMostBoringSink = Sink.ignore

  val forEachSink = Sink.foreach[String](println)

  val headSink = Sink.head[Int] // retrieves head and then closes the stream

  val foldSink = Sink.fold[Int, Int](0)(_ + _)

  // flows - usually mapped to collection operators
  val mapFlow = Flow[Int].map(_ * 2)

  val takeFlow = Flow[Int].take(5)

  // not have flatMap

  // source -> flow -> flow -> sink

  val source = Source(1 to 20)

  val doubleFlowGraph = source.via(mapFlow).via(takeFlow).via(mapFlow).to(sink)

//  doubleFlowGraph.run()

  // syntactic sugars
  val mapSource = Source(1 to 10).map(_ * 2) // Source(1 to 10).via(Flow[Int](_ * 2))

  // run streams directly
//  mapSource.runForeach(println) // mapSource.to(Sink.foreach[Int](println)).run()

  // operators = components

  // exercise
  // create a stream that takes the names of persons, first two names, with length > 5 characters

  Source(List("joze", "polde", "franci", "benjamin", "gozdni joza", "roxy")).filter(_.length > 5).take(2).runForeach(println)
}
