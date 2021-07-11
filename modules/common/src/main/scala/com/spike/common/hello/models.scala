package com.spike.common.hello

final case class Hello(id: Hello.Id, message: Hello.Message)
object Hello {
  final case class Id(id: String)
  final case class Message(value: String, timestamp: Long)
}

final case class HelloClientId(value: String) extends AnyVal

final case class HelloGroupId(value: String) extends AnyVal
