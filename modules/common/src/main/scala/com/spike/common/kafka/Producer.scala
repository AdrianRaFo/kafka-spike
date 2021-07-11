package com.spike.common.kafka

trait Producer[F[_], A] {
  def sendMessage(a: A): F[Unit]
}
