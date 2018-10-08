package job

sealed trait Message

object UpdateMessage extends Message
object SuccessMessage extends Message
object ErrorMessage extends Message
