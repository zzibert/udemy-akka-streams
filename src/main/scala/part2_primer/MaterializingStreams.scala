package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import scala.util.{Failure, Success}

object MaterializingStreams extends App {
  implicit val system = ActorSystem("MaterializingStreams")
  implicit val materializer = ActorMaterializer()

  val simpleGraph = Source(1 to 10).to(Sink.foreach(println))

//  val materializedValue = simpleGraph.run()

  val source = Source(1 to 10)
  val sink = Sink.reduce[Int](_ + _)

//  val sumFuture = source.runWith(sink)

  import system.dispatcher


  // choosing materialized values
  val simpleSource = Source(1 to 10)
  val simpleFlow = Flow[Int].map(_ + 1)
  val simpleSink = Sink.foreach[Int](println)

//  simpleSource.viaMat(simpleFlow)((sourceMat, flowMat) => flowMat)
  // same as
  val graph = simpleSource.viaMat(simpleFlow)(Keep.right).toMat(simpleSink)(Keep.right)
//  graph.run().onComplete {
//    case Success(value) => println(s"stream processing finished $value")
//    case Failure(exception) => println(s"stream processing failed $exception")
//  }

  // sugars
//  val sumFuture = Source(1 to 10).runWith(Sink.reduce[Int](_ + _))
//  sumFuture.onComplete {
//    case Success(value) => println(s"the sum of all elements is $value")
//    case Failure(exception) => println(s"the sum of the elements could not be computed $exception")
//  }

//  Source(1 to 10).runReduce(_ + _)  more sugar

  // run backwards
//  Sink.foreach[Int](println).runWith(Source.single(42))

//  Flow[Int].map(_ * 2).runWith(simpleSource, simpleSink)

  /*
  * Many different ways, many versions of runnable graphs
  * return the last element out of a source (use Sink.last)
  * - compute the total word count of a stream of sentences
  * - map , fold, reduce
  * */

  val sentences = List("sentence one", "sentence two", "sentence three")

//  val graph1 = Source(sentences).runWith(Sink.last[String])

//  graph1.onComplete {
//    case Success(value) => println(s"the last sentence is $value")
//    case Failure(exception) => println(s"error $exception")
//  }

  val wordCountSink = Sink.fold[Int, String](0)((currentValue, sentence) => currentValue + sentence.split(" ").length)

//  val f1 = Source(sentences).toMat(wordCountSink)(Keep.right).run()

//  val result = Source(sentences).map(_.split(" ").length).runReduce(_ + _)
//
//  result.onComplete {
//    case Success(value) => println(s"the number of words is $value")
//    case Failure(exception) => println(s"error $exception")
//  }

  val wordCountFlow = Flow[String].fold[Int](0)((currentValue, sentence) => currentValue + sentence.split(" ").length)

//  val g4 = Source(sentences).via(wordCountFlow).toMat(Sink.head)(Keep.right).run()
//
//  g4.onComplete {
//    case Success(value) => println(s"the number of words is $value")
//    case Failure(exception) => println(s"error $exception")
//  }

  val g7 = wordCountFlow.runWith(Source(sentences), Sink.head)._2

  g7.onComplete {
    case Success(value) => println(s"the number of words is $value")
    case Failure(exception) => println(s"error $exception")
  }

}
