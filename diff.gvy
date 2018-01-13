strA = """
def harmonic(n) {
  int x = 3
  int y = 4
  int z = 5
  String constant = ""
  return x + y
  // SOME COMMENT HERE
}
""".tokenize("\n")

strB = """
def harmonic(n) {
  String constant = ""
  return (1..n).collect { 1 / it }.sum()
  // SOME COMMENT HERE
}
""".tokenize("\n")

def red(str) {
  return "\u001B[31m$str\u001B[0m"
}

def green(str) {
  return "\u001B[32m$str\u001B[0m"
}

def diff(a, b) {
  if (a.size() == b.size() && a.size() == 0) {
    return [0, [:]]
  }
  if (a.size() == 0) {
    return [b.size(), b.withIndex().collectEntries { line, num -> ["add-$num": line] }]
  }
  if (b.size() == 0) {
    return [a.size(), a.withIndex().collectEntries { line, num -> ["remove-$num": line] }]
  }
  if (a.last() == b.last()) {
    return diff(a.dropRight(1), b.dropRight(1))
  }
  def val = [
    diff(a, b.dropRight(1)).with { it[1]["add-${a.size()}"] = b.last(); it},
    diff(a.dropRight(1), b).with { it[1]["remove-${a.size()-1}"] = a.last(); it}
  ].min { it[0] }
  val[0]++
  return val
}
changes = diff(strA, strB)[1].sort { a, b ->
   b.getKey().split("-")[1] <=> a.getKey().split("-")[1] ?:
   b.getKey().split("-")[0] <=> a.getKey().split("-")[0]
}
println "----BEGIN DIFF----"
changes.each {
  key = it.getKey()
  if (key.contains("remove")) {
    strA[key.split("-")[1].toInteger()] = red(strA[key.split("-")[1].toInteger()])
  } else {
    strA = strA.plus(key.split("-")[1].toInteger(), green(it.getValue()))
  }
}
println strA.join("\n")
println "----END DIFF----"
