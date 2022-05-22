# defer-kt

Golang-inspired resource management library.

## Add to your project

### Gradle

```gradle
implementation "me.jason5lee:defer:1.0.1"
```

### Gradle Kotlin DSL

```kotlin
implementation("me.jason5lee:defer:1.0.1")
```

## Example

Open two file for reading and writing, print a message before attempting to close them.

```kotlin
deferScope {
    val f1 = File("/path/to/read-file").bufferedReader()
    deferClosing(f1)
    val f2 = File("/path/to/write-file").bufferedWriter()
        .also { deferClosing(it) }
    defer { println("Closing file ...") }
    // Operating f1 and f2
}
```

Equivilent code using `use` and `try-finally`

```kotlin
File("/path/to/read-file").bufferedReader().use { f1 ->
    File("/path/to/write-file").bufferedWriter().use { f2 ->
        try {
            // Operating f1 and f2, with 3 indents instead of 1.
        } finally {
            println("Closing file ...")
        }
    }
}
```

Example of multiple resources and canceling: suppose you have many output targets, each target has a name. Now you want to implement an object that maintains multiple output target objects, which output information to the corresponding target according to the type of information. You can implement this object like this

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
                // This defer is to close the created target when an exception is thrown
                deferClosing(target)
                outMap[name] = target
            }
            outMap.toMap().also {
                cancelDefers() // cancel all defers because the construction was successful.
            }
        }
    }

    fun output(message: Message) {
        outputMap[message.type]?.output(message)
    }

    override fun close() {
        // You can use deferScope to ensure all targets are closed even if some fail.
        deferScope {
            for (output in outputMap.values) {
                deferClosing(output)
            }
        }
    }
}
```

## Advantage over `use` and `try-finally`

Compared to using the use function and try-finally, this library has the following advantages

1. Less indent, especially if you have multiple resources to close.
2. Easy to have a variable number of resources.
3. Provide the `cancelDefer` method to transfer ownership, i.e. pass the resource to another object and let it close.
