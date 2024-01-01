package me.jason5lee.defer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.*

class TestSuspendDefer {
    @Test
    fun testNormalWorkflow() = runTest {
        assertEquals(10, suspendDeferScope { 10 })
    }

    @Test
    fun testDeferOrder() = runTest {
        var current = 0
        suspendDeferScope {
            defer {
                // The second defer is executed.
                assertEquals(2, current)
                current = 3
            }
            defer {
                // The scope is executed but the first defer is not executed.
                assertEquals(1, current)
                current = 2
            }
            // No defer is executed yet.
            assertEquals(0, current)
            current = 1
        }
        // All defers should be executed.
        assertEquals(3, current)
    }

    @Test
    fun testException() = runTest {
        var current = 0

        val exception = try {
            suspendDeferScope {
                defer {
                    // The second defer is executed.
                    assertEquals(2, current)
                    current = 3
                    throw Throwable("2")
                }
                defer {
                    // The scope is executed but the first defer is not executed.
                    assertEquals(1, current)
                    current = 2
                    throw Throwable("1")
                }
                // No defer is executed yet.
                assertEquals(0, current)
                current = 1
                throw Throwable("0")
            }
        } catch (e: Throwable) {
            e
        }

        assertEquals(3, current)
        assertEquals("0", exception.message)
        // This test won't work because suppressed exceptions are not preserved across context in test.
        // https://github.com/Kotlin/kotlinx.coroutines/blob/master/docs/topics/debugging.md#stacktrace-recovery
        // assertEquals(listOf("1", "2"), exception.suppressedExceptions.map { it.message })
    }

    @Test
    fun testCancelDeferred() = runTest {
        suspendDeferScope {
            defer {
                assertTrue(false, "cancelled deferred task shouldn't be executed")
            }
            cancelDeferred()
        }
    }

    @Test
    fun testUseAfterCancelDeferred() = runTest {
        suspendDeferScope {
            cancelDeferred()
            assertFailsWith<IllegalStateException>("defer should fail after cancelling") {
                defer { assertTrue(false, "this shouldn't be executed after being cancelled") }
            }
        }
    }

    @Test
    fun testOutOfScope() = runTest {
        var outSuspend: SuspendDeferScope? = null
        suspendDeferScope {
            outSuspend = this
        }
        assertFailsWith<IllegalStateException>("defer should fail when calling out of scope") {
            outSuspend?.defer {
                assertTrue(false, "this shouldn't be executed out of scope")
            }
        }
    }

    @Test
    fun testCancellation() = runTest {
        val blockChannel = Channel<Unit>(0) // Channel that never receives anything. It's used to block the coroutine.
        val commChannel = Channel<Unit>(1) // Channel to communicate between main test and defer scope coroutine.

        val job = launch {
            suspendDeferScope {
                defer {
                    yield()
                    // For suspendDeferScope, `yield` won't cause cancellation exception even the coroutine is cancelled.
                    commChannel.send(Unit)
                }
                commChannel.send(Unit)
                blockChannel.receive()
            }
        }
        commChannel.receive()
        try {
            job.cancelAndJoin()
        } catch (_: CancellationException) {
        }
        assertTrue(commChannel.tryReceive().isSuccess, "deferred task should be not be cancelled")
    }
}
