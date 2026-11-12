package dev.slne.surf.nats.test.paper

import com.google.auto.service.AutoService
import dev.slne.surf.nats.test.common.NatsTestInstance

@AutoService(NatsTestInstance::class)
class PaperNatsTestInstanceImpl : NatsTestInstance()