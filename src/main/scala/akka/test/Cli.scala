package akka.test

import akka.actor._
import akka.contrib.pattern.ClusterClient
import akka.routing.ConsistentHashingRouter.ConsistentHashableEnvelope
import akka.test.Messages.SchedulerMessage
import java.util.UUID

/**
 * User: Eugene Dzhurinsky
 * Date: 5/6/14
 */
object Cli {

  def main(args: Array[String]) {
    val system = ActorSystem("HttpCluster")
    val c = system.actorOf(ClusterClient.props(Set(
      system.actorSelection("akka.tcp://HttpCluster@127.0.0.1:2551/user/receptionist")
    )))
    val ref = system.actorOf(Props(new Controller(c)), "controller")
    for (i <- 1 to 1) {
      Thread.sleep(2000)
      println("Send message " + i)
      ref ! SchedulerMessage(UUID.randomUUID(), io.Source.fromInputStream(Cli.getClass.getResourceAsStream("/application.conf")).mkString)
    }
  }

  trait LocalState

  class Controller(client: ActorRef) extends Actor with ActorLogging {

    override def receive = {
      case (req: SchedulerMessage) ⇒
        log.info("Sending request: {}", req)
        client ! ClusterClient.Send("/user/router_scheduler", ConsistentHashableEnvelope(req, 17), true)
    }

  }

}