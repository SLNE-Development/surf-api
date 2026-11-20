package dev.slne.surf.api.paper.sharedbean

import dev.slne.surf.api.paper.plugin.SurfPaperBootstrapper

/**
 * The consumer's bootstrapper.
 *
 * It sits in the package both test plugins share, so the consumer's component scan covers the
 * provider's package too. Only the scan's restriction to this jar keeps the provider's beans out.
 */
class SharedBeanConsumerBootstrapper : SurfPaperBootstrapper()
