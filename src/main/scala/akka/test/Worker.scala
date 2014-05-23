package akka.test

import akka.actor.{ActorSystem, Actor, ActorLogging}
import com.typesafe.config.ConfigFactory
import akka.test.Messages.{WorkerMessageResponse, WorkerMessage}
import akka.test.PullTask.{WorkerRegistration, MasterRegistration, GiveMeWork, AnnounceWorkAvailable}

/**
 * User: Eugene Dzhurinsky
 * Date: 5/23/14
 */
object Worker {

  class WorkerActor extends Actor with ActorLogging {

    override def preStart() {
      log.info("Starting up")
    }

    override def receive = {
      case WorkerMessage(payLoad) ⇒
        log.info("Got message: {}", payLoad)
        sender() ! WorkerMessageResponse(payLoad.toUpperCase)
      case AnnounceWorkAvailable ⇒
        sender() ! GiveMeWork
      case MasterRegistration ⇒
        sender() ! WorkerRegistration
      case xxx ⇒ println("GOT message " + xxx + " from " + sender())
    }

  }

  def main(args: Array[String]) {
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [worker]")).
      withFallback(ConfigFactory.load("application"))
    ActorSystem("HttpCluster", config)
  }


}