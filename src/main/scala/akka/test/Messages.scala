package akka.test

import java.util.UUID
import akka.routing.ConsistentHashingRouter.ConsistentHashable

/**
 * User: Eugene Dzhurinsky
 * Date: 5/23/14
 */
object Messages {

  case class WorkerMessage(word: String)

  case class WorkerMessageResponse(data: String)

  case class SchedulerMessage(id: UUID, phrase: String) extends ConsistentHashable {
    override def consistentHashKey: Any = (id, phrase)
  }

}
