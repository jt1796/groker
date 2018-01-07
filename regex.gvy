// each 'feature' of regex has a method that returns a closure.
//    The closure takes in a str, and returns a list of possible remainders

def lit(str) {
    return { (it.startsWith(str)) ? [it.substring(str.length())] : [] }
}

def star(pat) {
    return { it, remainders=[] ->
        toCompute = ([it] + pat(it)).findAll { r -> !remainders.contains(r) }
        remainders += toCompute
        (toCompute.collect{ s -> star(pat)(s, remainders) }.flatten() + remainders).unique()
    }
}

def dot() {
    return { it.length() > 0 ? [it.substring(1)] : [] }
}

def oneof(Object... pats) {
    return { pats.collect { pat -> pat(it)}.flatten() }
}

def seq(Object... pats) {
    return {
        pats.size() == 0 ? [it] : pats[0](it).collect { r -> seq(pats.drop(1))(r) }.flatten()
    }
}

def opt(pat) {
    return { [it] + pat(it) }
}

def eol() {
    return { it == '' ? [''] : [] }
}

def digit() {
    return {
        def isDigit = it.length() > 0 && ('0' <= it[0]) && ('9' >= it[0])
        isDigit ? [it.substring(1)] : []
    }
}

// SMALL TESTS //
assert lit('walk the dog')('walk the dog to the park') == [' to the park']
assert lit('abc')('xyz') == []
assert star(lit('a'))('aab') == ['aab', 'ab', 'b']
assert dot()('abc') == ['bc']
assert digit()('909') == ['09']
assert digit()('A909') == []
assert oneof(lit('x'), lit('y'), lit('z'))('ybc') == ['bc']
assert seq(lit('x'), lit('y'))('xyz') == ['z']
assert seq(lit('a'))('a') == ['']
assert seq(lit('a'), lit('b'))('ab') == ['']
assert eol()('') == ['']
assert eol()('not end of line') == []
assert seq(star(dot()), eol())('any') == ['']
assert opt(lit('a'))('az') == ['az', 'z']
assert star(opt(lit('a')))('aaa') == ['aaa', 'aa', 'a', '']


// [.*](((\ =\ )|(\ :=\ ))(.*))?
matcher = seq(
            lit('['),
            star(dot()),
            lit(']'),
            opt(
              seq(
                oneof(lit(' = '), lit(' := ')),
                star(dot())
              )
            ),
            eol()
          )

assert matcher('[abcd]') == ['']
assert matcher('[abdce') == []
assert matcher('[file] = /var/log/program.txt') == ['']
assert matcher('[file] := /var/log/program.txt') == ['']
assert matcher('[file]=/var/program.txt') == []


// \d*[.]\d*
matcher = seq(
           star(digit()),
           lit("."),
           star(digit()),
           eol()
         )

assert matcher('9.90') == ['']
assert matcher('9e4') == []
