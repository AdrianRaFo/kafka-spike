package com.spike.fs2kafka.kafka

import com.spike.common.hello.Hello
import cats.implicits._
import org.apache.avro.generic.GenericRecord
import vulcan.Codec
import vulcan.Codec.Aux

object implicits {

  implicit val idFromRecord: Aux[GenericRecord, Hello.Id] = Codec.record(
    name = "Hello.Id",
    namespace = "com.spike.fs2kafka"
  ) { field =>
    field("id", _.id).map(Hello.Id)
  }

  implicit val messageToRecord: Aux[GenericRecord, Hello.Message] = Codec.record(
    name = "Hello.Message",
    namespace = "com.spike.fs2kafka"
  ) { field =>
    (
      field("value", _.value),
      field("timestamp", _.timestamp)
    ).mapN(Hello.Message)
  }

}
