package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, BidiShape, ClosedShape}
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}

object BidirectionalFlows extends App {
  implicit val system = ActorSystem("BidirectionalFlows")
  implicit val materializer = ActorMaterializer()

  /*
  * Example: Cryptography
  * */

  def encrypt(n: Int)(str: String): String = str.map(c => (c  + n).toChar)

  def decrypt(n: Int)(str: String): String = str.map(c => (c  - n).toChar)

//  println(encrypt(3)("Akka"))
//  println(decrypt(3)(encrypt(3)("Akka")))

  // bidirectional flow

  val bidiCryptoStaticGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val encyptionFlowShape = builder.add(Flow[String].map(encrypt(3)))
    val decryptionFlowShape = builder.add(Flow[String].map(decrypt(3)))

//    BidiShape(encyptionFlowShape.in, encyptionFlowShape.out, decryptionFlowShape.in, decryptionFlowShape.out)
    BidiShape.fromFlows(encyptionFlowShape, decryptionFlowShape)
  }

  val unencryptedStrings = List("akka", "is", "awesome", "testing", "bidirectional", "flows")

  val unencryptedSource = Source(unencryptedStrings)

  val encryptedSource = Source(unencryptedStrings.map(encrypt(3)))

  val cryptoBidiGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val bidiGraph = builder.add(bidiCryptoStaticGraph)
      val unencryptedSourceShape = builder.add(unencryptedSource)
      val encryptedSourceShape = builder.add(encryptedSource)
      val encryptedSinkShape = builder.add(Sink.foreach[String](x => println(s"Encrypted $x")))
      val decryptedSinkShape = builder.add(Sink.foreach[String](x => println(s"Decrypted $x")))

      unencryptedSourceShape ~> bidiGraph.in1 ; bidiGraph.out1 ~> encryptedSinkShape
      decryptedSinkShape <~ bidiGraph.out2 ; bidiGraph.in2 <~ encryptedSourceShape


      ClosedShape
    }
  )

  cryptoBidiGraph.run()
}
