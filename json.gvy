String grammar = '''
  VALUE     -> STRING | ARRAY | OBJECT | NUM | BOOL

  OBJECT    -> {} | { PAIR (, PAIR)* }
  PAIR      -> STRING : VALUE

  ARRAY     -> [] | [ VALUE | (, VALUE)* ]

  NUM       -> '-'? INT ('.' [0-9]+)? EXP?
  INT       -> '0' | [1-9][0-9]*
  EXP       -> [Ee] [+-]? INT

  STRING    -> '"' (ESC | not ["\\])* '"'
  ESC       -> '\\' [\\/bfnrt]

  BOOL      -> 'true' | 'false'
'''

// rules return [tree fragments, remainder of text after chomping]
rules = [:]
rules['VALUE'] = { it ->
  return rules['BOOL'](it) ?:
         rules['STRING'](it) ?:
         rules['ARRAY'](it) ?:
         rules['OBJECT'](it) ?:
         rules['NUM'](it)
}
rules['BOOL'] = { it ->
  if (it.startsWith('true')) {
    return [true, it.substring(6)]
  }
  if (it.startsWith("'false'")) {
    return [false, it.substring(7)]
  }
}
rules['STRING'] = { it ->
  if (!it.startsWith('"')) {
    return null
  }
  int i = 0
  while (i++ < it.length()) {
    if (rules['ESC'](it.substring(i)) != null) {
      i += 1
      continue
    }
    if ('\\' == it[i]) {
      return
    }
    if (it[i] == '"') {
      return [it.substring(1, i), it.substring(i+1)]
    }
  }
}
rules['ESC'] = {
  if (it[0] == '\\' && "\\/bfnrt".indexOf(it[1]) > 0) {
    return [it.substring(0, 1), it.substring(2)]
  }
}
rules['OBJECT'] = { it ->
  def (obj, k, v) = [[:], null, null]
  if (!it.startsWith('{')) {
    return
  }
  it = it.substring(1)
  if (it.startsWith('}')) {
    return [obj, it.substring(1)]
  }
  (k, v, it) = rules['PAIR'](it)
  obj[k] = v
  while (it.startsWith(',')) {
    it = it.substring(1)
    (k, v, it) = rules['PAIR'](it)
    obj[k] = v
  }
  if (it.startsWith("}")) {
    return [obj, it.substring(1)]
  }
}
rules['PAIR'] = { it ->
  def val = null
  def (name, remainder) = rules['STRING'](it)
  if (remainder.startsWith(':')) {
    remainder = remainder.substring(1)
    (val, remainder) = rules['VALUE'](remainder)
  }
  return [name, val, remainder]
}
rules['ARRAY'] = { it ->
  def (arr, elem) = [[], null]
  if (!it.startsWith('[')) {
    return
  }
  it = it.substring(1)
  if (it.startsWith(']')) {
    return [arr, it.substring(1)]
  }
  (elem, it) = rules['VALUE'](it)
  arr << elem
  while (it.startsWith(',')) {
    it = it.substring(1)
    (elem, it) = rules['VALUE'](it)
    arr << elem
  }
  if (it.startsWith("]")) {
    return [arr, it.substring(1)]
  }
}
rules['NUM'] = { it ->
  if (!(it[0] =~ /[0-9]|-/)) {
    return
  }
  def (json_int, exponent, number, negative) = [null, null, 0, false]
  if (it[0] == '-') {
    negative = true
    it = it.substring(1)
  }
  (number, it) = rules['INT'](it)
  if (it[0] == '.') {
    it = it.substring(1)
    if (!it[0] =~ /[0-9]/) {
      return
    }
    int i = 0
    while (it[i++] =~ /0-9/)
      ;
    def decimal = Double.parseDouble(it.substring(0, i))
    while (decimal > 1) {
      decimal = decimal / 10
    }
    it = it.substring(i)
    number += decimal
  }
  def possible_exp = rules['EXP'](it)
  if (possible_exp) {
    it = possible_exp[1]
    number = number * 10 ** possible_exp[0]
  }
  if (negative) {
    number *= -1
  }
  return [number, it]
}
rules['INT'] = { it ->
  if (it[0] == '0') {
    return [0, substring(1)]
  }
  if (!(it[0] =~ /[1-9]/)) {
    return
  }
  for (int i = 1; i < it.length(); i++) {
    if (!(it[i] =~ /[0-9]/)) {
      return [Integer.parseInt(it.substring(0, i)), it.substring(i)]
    }
  }
}
rules['EXP'] = { it ->
  if (!(it[0] =~ /[Ee]/)) {
    return
  }
  it = it.substring(1)
  if (['+', '-'].contains(it[0])) {
    it = it.substring(1)
  }
  return rules['INT'](it)
}

// END OF PARSER CODE

def parse_json(String text) {
  text = text.replaceAll("\\s", "") + "EOF"
  def (tree, rem) = rules['VALUE'](text)
  if ('EOF' == rem) {
    println "valid json"
    return tree
  } else {
    println "invalid json"
  }
}

String sample_json = """
{
  "key": [
    {
      "nestedObject": 23
    },
    {
      "anotherOne": [
        11,
        -1,
        1.0,
        -3.5e2
      ]
    }
  ],
  "keyTwo": "valTwo \\t with an escape"
}
"""
assert parse_json(sample_json)["key"][1]["anotherOne"][3] == -350.0
