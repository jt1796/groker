String grammar = '''
  VALUE     -> STRING | ARRAY | OBJECT | NUM | 'true' | 'false'

  OBJECT    -> {} | { PAIR (, PAIR)* }
  PAIR      -> STRING : VALUE

  ARRAY     -> [] | [ VALUE | (, VALUE)* ]

  NUM       -> '-'? INT ('.' [0-9]+)? EXP?
  INT       -> '0' | [1-9][0-9]*
  EXP       -> [Ee] [+-]? INT

  STRING    -> '"' (ESC | not ["\\])* '"'
  ESC       -> '\\' [\\/bfnrt]
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
  for (int i = 1; i < it.length(); i++) {
    if (it[i] == '"') {
      return [it.substring(1, i), it.substring(i+1)];
    }
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
  if (!(it[0] =~ /[1-9]/)) {
    return
  }
  for (int i = 1; i < it.length(); i++) {
    if (!(it[i] =~ /[0-9]/)) {
      return [Integer.parseInt(it.substring(0, i-1)), it.substring(i)]
    }
  }
}

def parse_json(String text) {
  text = text.replaceAll("\\s", "") + "EOF"
  def (tree, rem) = rules['VALUE'](text)
  if ('EOF' == rem) {
    println "valid json"
    println tree
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
        "elemone",
        "elemtwo",
        "elemthree"
      ]
    }
  ],
  "keyTwo": "valTwo"
}
"""

parse_json(sample_json)
