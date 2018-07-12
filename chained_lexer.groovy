str = """
  123213 + 321
  x = 3; y = 7
  print x
"""

def csplit(txt, pat, label) {
  outer = txt.split(pat).toList()
  inner = (txt =~ pat).collect { it }
  ret = []
  while(txt != "") {
    if (outer.size() > 0 && txt.startsWith(outer.head())) {
      txt = txt.substring(outer.head().length())
      ret << outer.removeAt(0)
    }
    if (inner.size() > 0 && txt.startsWith(inner.head())) {
      txt = txt.substring(inner.head().length()) 
      ret << [inner.removeAt(0).replaceAll("\n", "nl"), label]
    }
  }
  return ret.findAll { it != "" }
}

def charstream(String s) {
  def data = s.toCharArray().toList()
  return {
    return data.size() == 0 ? "" : data.removeAt(0)
  }
}

def tokens(input, pattern, label) {
  boolean finished = false
  String buf = ""
  def tokens = []
  return {
    while(!finished) {
      def i = input()
      finished = i == ""
      if (i instanceof List) {
        tokens.addAll csplit(buf, pattern, label)
        tokens << i
        buf = ""
      } else {
        buf += i
        def splitted = csplit(buf, pattern, label)
        if (splitted.size() > 2 || i == "") {
          tokens.addAll splitted
          buf = ""
        }
      }
    }
    return tokens.size() == 0 ? "" : tokens.removeAt(0)
  }
}

def makelexer(...pieces) {
    return { str -> pieces.toList().inject(charstream(str), { a, b -> tokens(a, *b) })}
}

def lexer = makelexer(
  [/;|\n/,     'SEP'], 
  [/print/,    'PRT'],
  [/[a-z]+/,   'ID'],
  [/\d+/,      'INT'],
  [/\+|\-|=/,  'OP'],
  [/\s+/,      'WS']
)(str)

30.times { println lexer() }
