# defer-kt

Golang-inspired resource management library.

## Advantage over `use` and `try-finally`

Comparing to the `use` function in Kotlin standard library and `try-finally` block, this library provides the following advantages.

1. Less indentation, especially when you have multiple resource that need to be closed.
2. Easy to have a variable number of resources.
3. Provide a `cancelDefer` method to move ownership, that is, pass the resource to another object and let it take care of closing.

For example, you have some output targets, each of them associated with a name.
Now you want to implement an object that maintain multiple output targets, and output a message to the corresponding target
based on the message's type. You can do it with this library like this.

```kotlin
data class Message(val type: String, ...)

class OutputTarget(name: String): Closeable {
    override fun close() { ... }
    fun output(message: Message) { ... }
}

class Output(vararg names: String) : Closeable {
    private val outputMap: Map<String, OutputTarget>

    init {
        outputMap = deferScope {
            val outMap = HashMap<String, OutputTarget>(names.size)

            for (name in names) {
                val target = OutputTarget(name)
                deferClosing(target) // This defer is for the case that any exception occurs during the constructions.
                outMap[name] = target
            }
            outMap.toMap().also {
                cancelDefers() // Cancel all defers since the construction succeeds.
            }
        }
    }

    fun output(message: Message) {
        outputMap[message.type]?.output(message)
    }

    override fun close() {
        // You can also use defer to ensure that all closing are executed even some of them fails.
        deferScope {
            for (output in outputMap.values) {
                deferClosing(output)
            }
        }
    }
}
```