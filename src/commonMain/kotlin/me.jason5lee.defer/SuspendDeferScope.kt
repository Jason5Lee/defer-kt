package me.jason5lee.defer

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * A defer scope for suspendable deferred tasks. It is not thread-safe.
 */
public class SuspendDeferScope @PublishedApi internal constructor() {
    private var defers: ArrayList<suspend () -> Unit>? = ArrayList()

    /**
     * Add a deferred task.
     *
     * @throws DeferScopeClosedException if out of scope or cancelled.
     */
    public fun defer(task: suspend () -> Unit) {
        (defers ?: throw DeferScopeClosedException(scopeName = "SuspendDeferScope")).add(task)
    }

    /**
     * Add a deferred task which operates [this].
     *
     * @return [this] object.
     * @throws DeferScopeClosedException if out of scope or cancelled.
     */
    public inline fun <T> T.defer(crossinline task: suspend T.() -> Unit): T {
        this@SuspendDeferScope.defer { task(this) }
        return this@defer
    }

    /**
     * Cancel all deferred tasks.
     */
    public fun cancelDeferred() {
        defers = null
    }

    private suspend fun runDeferred() {
        val defers = this.defers ?: return
        var exception: Throwable? = null
        for (i in (defers.size - 1) downTo 0) {
            try {
                defers[i]()
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
    internal suspend fun runDeferredNonCancellable() {
        withContext(NonCancellable) {
            runDeferred()
        }
    }

    private suspend fun runDeferred(exception: Throwable): Nothing {
        this.defers?.let { defers ->
            for (i in (defers.size - 1) downTo 0) {
                try {
                    defers[i]()
                } catch (e: Throwable) {
                    exception.addSuppressed(e)
                }
            }
        }
        this.defers = null

        throw exception
    }

    @PublishedApi
    internal suspend fun runDeferredNonCancellable(exception: Throwable): Nothing =
        withContext(NonCancellable) {
            runDeferred(exception)
        }
}

/**
 * Creates a defer scope. Within this scope, you can use `defer` to add deferred tasks.
 * When exiting the defer scope, these tasks are executed in reverse order of their addition.
 *
 * The deferred tasks are executed in a [NonCancellable] context. This is useful for clean-up operations,
 * ensuring they are performed regardless of the coroutine's cancellation state.
 * This requires `kotlinx-coroutines-core` dependency for coroutine context API.
 */
public suspend inline fun <R> suspendDeferScope(block: SuspendDeferScope.() -> R): R {
    val scope = SuspendDeferScope()
    val result = try {
        block(scope)
    } catch (e: Throwable) {
        scope.runDeferredNonCancellable(e)
    }
    scope.runDeferredNonCancellable()
    return result
}
