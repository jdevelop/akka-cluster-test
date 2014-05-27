package akka.test

import akka.actor.{Props, ActorSystem, Actor, ActorLogging}
import akka.contrib.pattern.ClusterReceptionistExtension
import akka.routing.FromConfig
import com.typesafe.config.ConfigFactory
import akka.test.Messages.{WorkerMessageResponse, WorkerMessage, SchedulerMessage}
import akka.test.Worker.WorkerActor

/**
 * User: Eugene Dzhurinsky
 * Date: 5/23/14
 */
object Scheduler {

  class SchedulerActor extends Actor with ActorLogging with PullTask {

    var currentTasks: Iterator[String] = Iterator.empty

    override def hasMoreTasksInQueue = !currentTasks.isEmpty

    override def nextTask: Option[WorkerMessage] = if (currentTasks.hasNext) Some(WorkerMessage(currentTasks.next())) else None

    override def preStartRouter = {
      context.actorOf(FromConfig.props(Props[WorkerActor]), "router_chunkworker")
    }

    type TaskData = WorkerMessage

    override def receive = {
      case SchedulerMessage(id, msg) ⇒
        log.info("Received message: {} ⇒ {}", id, msg)
        currentTasks = msg.split("\\s+").toList.iterator
        notifyChildrenHaveWork()
      case WorkerMessageResponse(src) ⇒
        log.info("Received {}", src)
        taskComplete(sender())
        if (isComplete) {
          log.info("Complete all")
        }
      case x ⇒ handleReceive(x)
    }

  }

  def main(args: Array[String]) {
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=2551").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [scheduler]")).
      withFallback(ConfigFactory.load("application"))
    val system = ActorSystem("HttpCluster", config)
    val svc = system.actorOf(FromConfig.props(Props[SchedulerActor]), "router_scheduler")
    ClusterReceptionistExtension(system).registerService(svc)
  }

}