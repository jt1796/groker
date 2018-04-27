/*
  This implementation supports backrefs, and so much of the code here is "tricky".
  Since this doesn't use a DFA, do not expect this to be performant.
  Look towards the bottom of this file to see tests/example usages.

  Each "feature" of the regex (such as `seq`) has a function that is invoked. In the case of seq(), it accepts
    as arguments the patterns to use in _seq_uence. seq() will then return a closure. This closure is to be called with
    the pattern once, then an iterator is returned. Each invocation of the iterator will return a possible "remainder" after the matching.
    Once all possible remainders are used, a null will be returned.

    Example: oneof(lit("a"), lit("b"), lit("c")) will return a closure c.
             Invoke closure c with the string "cat" and you will get an iterator.
             Invoke the iterator once and you get "at". Invoke again and you get null.

  Mine somehow seems to be faster than grep's in this case:
  groovy -e 'println "1" * 100' | grep -E '^(11+)(\1)+$'
*/

captures = []

def capture(pat) {
    return { str ->
        def iter = pat(str)
        def my_capture = null
        def indexAt = captures.size()
        captures << my_capture
        return {
            def val = iter()
            if (val) {
                captures[indexAt] = str.substring(0, str.length() - val.length())
            }
            return val
        }
    }
}

def backref() {
    return { str ->
        def capture_to_inspect
        def iter
        return {
            if (captures.size() != 0 && null == captures.last()) {
                return null
            }
            if (capture_to_inspect != captures.last()) {
                capture_to_inspect = captures.last()
                iter = lit(capture_to_inspect)(str)
            }
            return iter()
        }
    }
}

def lit(str) {
    return {
        def rem = (it.startsWith(str)) ? it.substring(str.length()) : null
        def returnVal = rem
        return {
            (returnVal, rem) = [rem, null]
            return returnVal
        }
    }
}

// going to be hard
def star(pat) {
    return { arg ->
        def computed = [].toSet()
        def frontier = []
        return {
            if (computed.isEmpty()) {
                frontier << arg
            }
            while (frontier != []) {
                def potential = frontier[0]
                def iter = pat(potential)
                while (null != potential) {
                    if (!computed.contains(potential)) {
                        frontier << potential
                        computed << potential
                        return potential
                    }
                    potential = iter()
                }
                frontier = frontier.drop(1)
            }
            return null
        }
    }
}

def dot() {
    return {
        def rem = (it.length() > 0) ? it.substring(1) : null
        def returnVal
        return {
            (returnVal, rem) = [rem, returnVal]
            return returnVal
        }
    }
}

def oneof(Object... pats) {
    return { str ->
        def pat_i = 0
        def iter = pats[pat_i](str)
        return {
            while (null != iter) {
                def possible
                while (null != (possible = iter())) {
                    return possible
                }
                if (++pat_i < pats.length) {
                    iter = pats[pat_i](str)
                } else {
                    iter = null
                }
            }
            return null
        }
    }
}

//this proved to be really hard
def seq(Object... pats) {
    return { str ->
        if (pats.length == 0) {
            def val = str
            def returnVal
            return {
                (val, returnVal) = [returnVal, val]
                return returnVal
            }
        }
        def iter = pats[0](str)
        def cur = iter()
        def child
        return {
            if (null == cur) {
                return null
            }
            if (null == child) {
                child = seq(pats.drop(1))(cur)
            }
            def potential = child()
            if (null != potential) {
                return potential
            }
            while (null != (cur = iter())) {
                child = seq(pats.drop(1))(cur)
                while (null != (potential = child())) {
                    return potential
                }
            }
            return null
        }
    }
}

def opt(pat) {
    return { str ->
        def iter = pat(str)
        def ret = str
        def returnVal
        return {
            (ret, returnVal) = [iter(), ret]
            return returnVal
        }
    }
}

def eol() {
    return { str ->
        def val = str == '' ? '' : null
        def returnVal
        return {
            (val, returnVal) = [returnVal, val]
            return returnVal
        }
    }
}

def digit() {
    return {
        def isDigit = it.length() > 0 && ('0' <= it[0]) && ('9' >= it[0])
        def rem = isDigit ? it.substring(1) : null
        def returnVal
        return {
            (returnVal, rem) = [rem, returnVal]
            return returnVal
        }
    }
}

def iter_to_list(iter) {
    def vals = []
    def toAdd
    while (null != (toAdd = iter())) {
        vals << toAdd
    }
    return vals
}

// SMALL TESTS //
assert iter_to_list(lit('walk the dog')('walk the dog to the park')) == [' to the park']
assert iter_to_list(lit('abc')('xyz')) == []
assert iter_to_list(star(lit('a'))('aaab')) == ['aaab', 'aab', 'ab', 'b']
assert iter_to_list(dot()('abc')) == ['bc']
assert iter_to_list(digit()('909')) == ['09']
assert iter_to_list(digit()('A909')) == []
assert iter_to_list(oneof(lit('x'), lit('yb'), dot())('ybc')) == ['c', 'bc']
assert iter_to_list(oneof(lit('x'), lit('y'), lit('z'))('ybc')) == ['bc']
assert iter_to_list(oneof(lit('x'), lit('z'), lit('y'))('ybc')) == ['bc']
assert iter_to_list(seq(lit('x'), lit('y'))('xyz')) == ['z']
assert iter_to_list(seq(lit('a'))('a')) == ['']
assert iter_to_list(seq(lit('a'), lit('b'))('ab')) == ['']
assert iter_to_list(eol()('')) == ['']
assert iter_to_list(eol()('not end of line')) == []
assert iter_to_list(seq(star(dot()), eol())('any')) == ['']
assert iter_to_list(opt(lit('a'))('az')) == ['az', 'z']
assert iter_to_list(star(opt(lit('a')))('aaa')) == ['aaa', 'aa', 'a', '']


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

assert iter_to_list(matcher('[abcd]')) == ['']
assert iter_to_list(matcher('[abdce')) == []
assert iter_to_list(matcher('[file] = /var/log/program.txt')) == ['']
assert iter_to_list(matcher('[file] := /var/log/program.txt')) == ['']
assert iter_to_list(matcher('[file]=/var/program.txt')) == []


// same as above, but  With Backref
matcher = seq(
            lit('['),
            capture(star(dot())),
            lit(']'),
            seq(
              lit('='),
              backref()
            ),
            eol()
          )

captures = []
assert iter_to_list(matcher('[something]=something')) == ['']


// \d*[.]\d*
matcher = seq(
           star(digit()),
           lit("."),
           star(digit()),
           eol()
         )

assert iter_to_list(matcher('9.90')) == ['']
assert iter_to_list(matcher('9e4')) == []

// (.*)\1
doubler = seq(
            capture(star(dot())),
            backref(),
            eol()
          )
assert iter_to_list(doubler("abcabc")) == [""]
assert iter_to_list(doubler("abcdef")) == []

// ((11)*)\1
iseven = seq(
          capture(
            star(lit("11"))
          ),
          backref().
          eol()
        )

(1..100).each {
    def expected = []
    if (it % 2 == 0) {
      expected = [""]
    }
    assert iter_to_list(iseven("1" * it)) == expected
}

//prime (111*)\1+
isprime = seq(
            capture(
              seq(
                lit("11"),
                star(lit("1"))
              )
            ),
            seq(backref(), star(backref())),
            eol()
          )

(1..100).each {
    def is_prime = { num -> (num <= 2) || !(2..Math.sqrt(num)).any { num % it == 0 } }
    def verified_prime = is_prime(it)
    def regex_prime = (iter_to_list(isprime("1" * it)).size() == 0)
    assert verified_prime == regex_prime
}
