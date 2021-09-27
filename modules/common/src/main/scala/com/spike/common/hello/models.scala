package com.spike.common.hello

object Hello {
  final case class Id(id: String)
  final case class Message(value: String, timestamp: Long)
}

final case class HelloClientId(value: String) extends AnyVal

final case class HelloGroupId(value: String) extends AnyVal
