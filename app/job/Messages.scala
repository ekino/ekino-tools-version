package job

sealed trait Message

object InitMessage extends Message
object UpdateMessage extends Message
object UpdateWithResponseMessage extends Message
object SuccessMessage extends Message
object StatusMessage extends Message
