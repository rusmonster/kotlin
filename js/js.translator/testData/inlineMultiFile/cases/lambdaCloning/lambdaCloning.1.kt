/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/lambdaTransformation/lambdaCloning.1.kt
 */

package foo

import test.*

fun test1(s: Long): String {
    return doSmth(s)
}

fun test2(s: Int): String {
    return doSmth2(s)
}

fun box(): String {
    var result = test1(11)
    if (result != "11") return "fail1: ${result}"

    result = test2(11)
    if (result != "11") return "fail2: ${result}"

    return "OK"
}