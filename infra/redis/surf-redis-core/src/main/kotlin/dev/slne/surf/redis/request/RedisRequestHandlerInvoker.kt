package dev.slne.surf.redis.request

fun interface RedisRequestHandlerInvoker {

    fun invoke(context: RequestContext<*>)
}