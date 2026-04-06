package dev.slne.surf.api.paper.util

import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory

inline fun <reified I> ReflectionProxyFactory.reflectionProxy(): I = reflectionProxy(I::class.java)