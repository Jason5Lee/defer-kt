package me.jason5lee.defer

public actual class DeferScope @PublishedApi internal actual constructor() {
    private var defers: ArrayList<() -> Unit>? = ArrayList()

    public actual fun defer(task: () -> Unit) {
        (defers ?: throwDeferScopeClosed()).add(task)
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
    internal actual fun runDefers(exception: Throwable): Nothing {
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
}