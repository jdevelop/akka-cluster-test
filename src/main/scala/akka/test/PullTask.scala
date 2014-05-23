package akka.test

import akka.actor.{ActorLogging, Actor, ActorRef}

import collection.mutable.{Map ⇒ MMap}
import scala.collection.mutable
import akka.actor.Terminated
import scala.Some
import akka.routing.Broadcast
import akka.test.PullTask._

/**
 * User: Eugene Dzhurinsky
 * Date: 5/16/14
 */
trait PullTask extends Actor with ActorLogging {

  type TaskData

  import scala.concurrent.duration._

  context.system.scheduler.schedule(1 second, 20 seconds, self, DiscoverChildren)(context.system.dispatcher)

  private val workers: MMap[ActorRef, WorkerState] = new mutable.HashMap[ActorRef, WorkerState]()

  private val failed: mutable.Queue[TaskData] = new mutable.Queue[TaskData]()

  var router: ActorRef = _

  var tasksInProgress = 0

  def hasMoreTasksInQueue: Boolean

  def hasMoreTasks = !failed.isEmpty || hasMoreTasksInQueue

  def nextTask: Option[TaskData]

  def notifyChildrenHaveWork() {
    log.debug("Send broadcast of new work to {}", workers)
    workers.filter(_._2 == Idle).foreach {
      case (worker, _) ⇒
        log.debug("Announce task to actor {}", worker)
        worker ! AnnounceWorkAvailable
    }
  }

  def isComplete = {
    log.debug("failed: {}, hasMoreTasks: {}, hasInProgress: {}", failed.isEmpty, hasMoreTasks, tasksInProgress)
    failed.isEmpty && !hasMoreTasks && tasksInProgress == 0
  }

  def taskComplete(worker: ActorRef) {
    log.debug("Setting worker {} state to Idle", worker)
    workers.put(worker, Idle)
    tasksInProgress = tasksInProgress - 1
  }

  def addWorker(worker: ActorRef) {
    log.debug("Adding worker {}", worker)
    if (!(workers contains sender())) {
      log.debug("Added worker {}", worker)
      context watch sender()
      workers.put(worker, Idle)
      if (hasMoreTasks) {
        worker ! AnnounceWorkAvailable
      }
    }
  }

  def removeWorker(worker: ActorRef) {
    log.debug("Removing worker {}", worker)
    def moveToFail(x: Any) = {
      failed.enqueue(x.asInstanceOf[TaskData])
      log.warning("Re-enqueue task {} for worker {}", x, worker)
    }
    workers.remove(worker) match {
      case Some(InProgress(x)) ⇒
        moveToFail(x)
      case _ ⇒
    }
    tasksInProgress = tasksInProgress - 1
  }

  def sendTask(worker: ActorRef) {
    (failed.dequeueFirst(_ ⇒ true) orElse nextTask) foreach {
      task ⇒
        worker ! task
        log.debug("Task {} sent to {}", task, worker)
        workers.put(worker, InProgress(task))
    }
  }

  def handleReceive: Actor.Receive = {
    case DiscoverChildren ⇒
      announceSelf()
    case MasterRegistration ⇒
      //log.debug("Registering master {}", sender())
      sender() ! WorkerRegistration
    case WorkerRegistration ⇒
      //log.debug("Registering worker actor {}", sender().path)
      addWorker(sender())
    case GiveMeWork ⇒
      sendTask(sender())
    case Terminated(ref) ⇒
      log.warning("Terminated actor {}", ref)
      removeWorker(ref)
  }

  def preStartRouter: ActorRef

  private def announceSelf() {
    //    log.debug("Broadcasting MasterRegistration message to {}", router)
    router ! Broadcast(MasterRegistration)
  }

  override def preStart() {
    log.debug("Starting actor {}", self)
    router = preStartRouter
    announceSelf()
  }

}

object PullTask {

  case object MasterRegistration

  case object WorkerRegistration

  case object GiveMeWork

  case object AnnounceWorkAvailable

  case object DiscoverChildren

  private sealed trait WorkerState

  private case object Idle extends WorkerState

  private case class InProgress[T](payload: T) extends WorkerState

}