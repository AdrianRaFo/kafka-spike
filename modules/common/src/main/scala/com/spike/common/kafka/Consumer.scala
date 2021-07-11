package com.spike.common.kafka

import fs2._

trait Consumer[F[_], A] {
  def deliveredMessages: Stream[F, A]
}
