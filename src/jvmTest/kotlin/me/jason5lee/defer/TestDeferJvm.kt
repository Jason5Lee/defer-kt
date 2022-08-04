package me.jason5lee.defer

import java.io.Closeable
import kotlin.test.Test
import kotlin.test.assertTrue

class Resource : Closeable {
    var closed: Boolean = false

    override fun close() {
        closed = true
    }

    fun read() {
        if (closed) {
            throw Exception("resource closed")
        }
    }
}

class TestDeferJvm {
    @Test
    fun testDeferClosing() {
        var out: Resource? = null
        deferScope {
            val r = Resource().deferClosing()
            r.read()
            out = r
        }
        assertTrue(out!!.closed)
    }
}
