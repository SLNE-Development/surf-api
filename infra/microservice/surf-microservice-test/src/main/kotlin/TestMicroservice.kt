import com.google.auto.service.AutoService
import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.surfapi.core.api.util.logger

@AutoService(Microservice::class)
class TestMicroservice : Microservice() {
    override val logger = logger()

    override suspend fun onBootstrap(args: List<String>) {
        logger.atInfo().log("Running onBootstrap")

        testCommand()
    }

    override suspend fun onDisable() {
        logger.atInfo().log("Running onDisable")
    }
}