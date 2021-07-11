package com.spike.kafka4s.kafka

import com.sksamuel.avro4s.Encoder.gen
import com.sksamuel.avro4s._
import com.spike.common.hello.Hello

object implicits {
  implicit val idFromRecord: FromRecord[Hello.Id] = FromRecord(AvroSchema[Hello.Id])
  implicit val idToRecord: ToRecord[Hello.Id] = ToRecord(AvroSchema[Hello.Id])

  implicit val messageFromRecord: FromRecord[Hello.Message] = FromRecord(AvroSchema[Hello.Message])
  implicit val messageToRecord: ToRecord[Hello.Message] = ToRecord(AvroSchema[Hello.Message])
}
