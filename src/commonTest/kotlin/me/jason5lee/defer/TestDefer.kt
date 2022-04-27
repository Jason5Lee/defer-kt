package me.jason5lee.defer

import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun testCancellation() {
        deferScope {
            defer {
                assertTrue(false, "cancelled defer shouldn't be executed")
            }
            cancelDefers()
        }
    }
}
