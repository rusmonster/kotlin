fun breakContinueInWhen(i: Int) {
    for (y in 0..10) {
        when(i) {
            0 -> <!BREAK_OR_CONTINUE_IN_WHEN!>continue<!>
            1 -> <!BREAK_OR_CONTINUE_IN_WHEN!>break<!>
            2 -> {
                for(z in 0..10) {
                    break
                }
                for(w in 0..10) {
                    continue
                }
            }
        }
    }
}


fun breakContinueInWhenWithWhile(i: Int, j: Int) {
    while (i > 0) {
        when (i) {
            0 -> <!BREAK_OR_CONTINUE_IN_WHEN!>continue<!>
            1 -> <!BREAK_OR_CONTINUE_IN_WHEN!>break<!>
            2 -> {
                while (j > 0) {
                    break
                }
            }
        }
    }
}

fun labeledBreakContinue(i: Int) {
    @outer for (y in 0..10) {
        when (i) {
            0 -> continue@outer
            1 -> break@outer
        }
    }
}
