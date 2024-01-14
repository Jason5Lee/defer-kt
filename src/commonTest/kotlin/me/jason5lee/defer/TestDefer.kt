package me.jason5lee.defer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestDefer {
    @Test
    fun testNormalWorkflow() {
        assertEquals(10, deferScope { 10 })
    }

    @Test
    fun testDeferOrder() {
        var current = 0
        deferScope {
            defer {
                // The second defer is executed.
                assertEquals(2, current)
                current = 3
            }
            2.defer { // 2 is this
                // The scope is executed but the first defer is not executed.
                assertEquals(1, current)
                current = this
            }.let { assertEquals(2, it) } // `this` is returned
            // No defer is executed yet.
            assertEquals(0, current)
            current = 1
        }
        // All defers should be executed.
        assertEquals(3, current)
    }

    @Test
    fun testException() {
        var current = 0

        val exception = try {
            deferScope {
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
        assertEquals(listOf("1", "2"), exception.suppressedExceptions.map { it.message })
    }

    @Test
    fun testCancelDeferred() {
        deferScope {
            defer {
                assertTrue(false, "cancelled deferred task shouldn't be executed")
            }
            cancelDeferred()
        }
    }

    @Test
    fun testUseAfterCancelDeferred() {
        deferScope {
            cancelDeferred()
            assertFailsWith<DeferScopeClosedException>("defer should fail after being cancelled") {
                defer { assertTrue(false, "this shouldn't be executed after being cancelled") }
            }
        }
    }

    @Test
    fun testOutOfScope() {
        var out: DeferScope? = null
        deferScope {
            out = this
        }
        assertFailsWith<DeferScopeClosedException>("defer should fail when calling out of scope") {
            out?.defer {
                assertTrue(false, "this shouldn't be executed out of scope")
            }
        }
    }
}
