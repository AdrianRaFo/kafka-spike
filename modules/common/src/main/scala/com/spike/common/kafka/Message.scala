package com.spike.common.kafka

final case class Message[K, V](key: K, value: V)