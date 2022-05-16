package me.jason5lee.defer

import java.io.Closeable

public actual class DeferScope @PublishedApi internal actual constructor() {
    private var defers: ArrayList<Closeable>? = ArrayList()

    public actual inline fun defer(crossinline task: () -> Unit) {
        deferClosing { task() }
    }

    public fun deferClosing(obj: Closeable) {
        (defers ?: throwDeferScopeClosed()).add(obj)
    }

    public actual fun cancelDefers() {
        defers = null
    }

    @PublishedApi
    internal actual fun runDefers() {
        val defers = this.defers ?: return
        var exception: Throwable? = null
        for (i in (defers.size - 1) downTo 0) {
            try {
                defers[i].close()
            } catch (e: Throwable) {
                if (exception == null) {
                    exception = e
                } else {
                    exception.addSuppressed(e)
                }
            }
        }
        this.defers = null
        if (exception != null) throw exception
    }

    @PublishedApi
    internal actual fun runDefers(exception: Throwable): Nothing {
        this.defers?.let { defers ->
            for (i in (defers.size - 1) downTo 0) {
                try {
                    defers[i].close()
                } catch (e: Throwable) {
                    exception.addSuppressed(e)
                }
            }
        }
        this.defers = null

        throw exception
    }
}