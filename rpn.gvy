inp = ['1', '2', '+', '4', '*']

def handle(it, stack, ast) {
  if (it =~ /[0-9]+/) {
    stack << it
    ast << it
  }
  if (it =~ /[+-\/\*]/) {
    a = stack.pop()
    b = stack.pop()
    stack << Eval.me("$b $it $a")
    a = ast.pop()
    b = ast.pop()
    ast << [b, it, a]
  }
}

stack = []
ast = []
inp.each { handle(it, stack, ast) }
println '' + ast + ' = ' + stack.pop()
