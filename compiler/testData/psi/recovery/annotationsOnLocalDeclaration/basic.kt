fun foo() {
    data class A

    data(123) object B {}

    data() object : Q {}

    d1 protected [d2] d3("") [d4() d5] val x1 = 1

    d6 var x2 = 2

    d7 private var x3 = 3

    d8 d9 val x4 = 4

    d10 fun bar1()
    d11 fun()

    [d12] d13 fun bar2()
    d14 [d15] fun bar3()
}
