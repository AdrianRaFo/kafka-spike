package com.spike.common.kafka

final case class Message[K, V](key: K, value: V)

final case class NextMessage[K, V](key: K, value: V)
