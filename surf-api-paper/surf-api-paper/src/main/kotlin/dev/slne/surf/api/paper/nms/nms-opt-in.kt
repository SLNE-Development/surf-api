package dev.slne.surf.api.paper.nms

@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This is delicate API and you should expect it to break at any time or require changes"
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class NmsUseWithCaution