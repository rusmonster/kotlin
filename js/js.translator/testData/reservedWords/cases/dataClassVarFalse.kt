package foo

// NOTE THIS FILE IS AUTO-GENERATED by the generateTestDataForReservedWords.kt. DO NOT EDIT!

data class DataClass(var `false`: String) {
    init {
        testNotRenamed("false", { `false` })
    }
}

fun box(): String {
    DataClass("123")

    return "OK"
}