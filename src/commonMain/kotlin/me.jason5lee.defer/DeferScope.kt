package me.jason5lee.defer

import io.ktor.utils.io.core.*

private fun throwDeferScopeClosed(): Nothing {
    throw IllegalStateException("DeferScope used out of scope or cancelled")
}

/**
 *
 */
public class DeferScope @PublishedApi internal constructor() {
    private var defers: ArrayList<Closeable>? = ArrayList()

    public inline fun defer(crossinline task: () -> Unit) {
        deferClosing(object : Closeable {
            override fun close() {
                task()
            }
        })
    }

    public fun deferClosing(obj: Closeable) {
        (defers ?: throwDeferScopeClosed()).add(obj)
    }

    public fun cancelDefers() {
        defers = null
    }

    @PublishedApi
    internal fun runDefers() {
        val defers = this.defers ?: return
        var exception: Throwable? = null
        for (i in (defers.size-1) downTo 0) {
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
    internal fun runDefers(exception : Throwable): Nothing {
        this.defers?.let { defers ->
            for (i in (defers.size-1) downTo 0) {
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

public inline fun <R> deferScope(block: DeferScope.() -> R): R {
    val scope = DeferScope()
    val result = try {
        block(scope)
    } catch (e: Throwable) {
        scope.runDefers(e)
    }
    scope.runDefers()
    return result
}
