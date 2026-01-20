package dev.slne.surf.redis.util

import reactor.core.Disposable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

abstract class DisposableAware : Disposable {
    private val disposables = ConcurrentHashMap.newKeySet<Disposable>()
    private val disposed = AtomicBoolean(false)

    final override fun isDisposed() = disposed.get()

    final override fun dispose() {
        if (!disposed.compareAndSet(false, true)) return
        dispose0()
        disposables.forEach(Disposable::dispose)
        disposables.clear()
    }

    protected abstract fun dispose0()

    protected fun trackDisposable(disposable: Disposable) {
        disposables.add(disposable)
    }
}