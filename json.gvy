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

// rules return remainder of text after chomping, or null if they do not apply
rules = [:]
rules['LIT'] = { it, text ->
  if (it.startsWith(text)) {
    return it.substring(text.length())
  }
}
rules['VALUE'] = { it ->
  return rules['LIT'](it, "'true'") ?:
         rules['LIT'](it, "'false'") ?:
         rules['STRING'](it) ?:
         rules['ARRAY'](it) ?:
         rules['OBJECT'](it) ?:
         rules['NUM'](it)
}
rules['STRING'] = { it ->
  if (!it.startsWith('"')) {
    return null
  }
  for (int i = 1; i < it.length(); i++) {
    if (it[i] == '"') {
      return it.substring(i+1);
    }
  }
}
rules['OBJECT'] = { it ->
  if (!it.startsWith('{')) {
    return
  }
  it = it.substring(1)
  if (it.startsWith('}')) {
    return it.substring(1)
  }
  it = rules['PAIR'](it)
  while (it.startsWith(',')) {
    it = rules['PAIR'](it)
  }
  if (it.startsWith("}")) {
    return it.substring(1)
  }
}
rules['PAIR'] = { it ->
  remainder = rules['STRING'](it)
  if (remainder.startsWith(':')) {
    remainder = remainder.substring(1)
    remainder = rules['VALUE'](remainder)
  }
  return remainder
}
rules['ARRAY'] = { it ->
  if (!it.startsWith('[')) {
    return
  }
  it = it.substring(1)
  if (it.startsWith(']')) {
    return it.substring(1)
  }
  it = rules['VALUE'](it)
  while (it.startsWith(',')) {
    it = rules['VALUE'](it)
  }
  if (it.startsWith("]")) {
    return it.substring(1)
  }
}
rules['NUM'] = { it ->
  if (!(it[0] =~ /[1-9]/)) {
    return
  }
  for (int i = 1; i < it.length(); i++) {
    if (!(it[i] =~ /[0-9]/)) {
      return it.substring(i)
    }
  }
}

def parse_json(String text) {
  text = text.replaceAll("\\s", "")
  if ('EOF' == rules['VALUE'](text + 'EOF')) {
    println "valid json"
  } else {
    println "invalid json"
  }
}

String sample_json = """
{
  "key": [
    {
      "nested object": 23
    }
  ]
}
"""

parse_json(sample_json)
