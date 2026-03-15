package dev.slne.surf.microservice.test.paper

import com.google.auto.service.AutoService
import dev.slne.surf.microservice.test.core.client.MessageClientInstance

@AutoService(MessageClientInstance::class)
class PaperMessageClientInstance : MessageClientInstance()