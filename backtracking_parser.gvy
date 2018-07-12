grammar = [
  'expr': [['atom', '+', 'expr'],
           ['atom', '*', 'expr'],
           ['(', 'expr', ')'],
           ['atom']],
  'atom': [['INT'],
           ['VAR']]
]

is_production = { grammar.collect { k, v -> k }.contains(it) }

//////



source = [
  ['3', 'INT'],
  ['+', '+'],
  ['4', 'INT'],
  ['*', '*'],
  ['(', '('],
  ['5', 'INT'],
  [')', ')']
]

def explore(token_index, unknown, tree_node) {
  if (token_index == source.size() || unknown == []) {
    return unknown == [] && token_index == source.size()
  }
  def frontier = unknown.removeAt(0)

  // frontier is a terminal
  if (!is_production(frontier)) {
    if (source[token_index][1] == frontier) {
      tree_node[frontier] = source[token_index][0]
      return explore(token_index + 1, unknown, tree_node)
    } else {
      unknown.add(0, frontier)
      return false
    }
  }

  // frontier is a nonterminal
  def found = false
  grammar[frontier].each { p ->
    def new_node = [:]
    if (explore(token_index, p + unknown, new_node)) {
      tree_node[frontier] = new_node
      found = true
      return
    }
  }
  unknown.add(0, frontier)
  return found
}

def print_tree(tree, prefix) {
  tree.keySet().toList().withIndex().each { newname, idx ->
    def newtree = tree[newname]
    def is_tail = idx == tree.size() - 1
    def marker = (is_tail ? "└── " : "├── ")
    println prefix + marker + newname
    if (newtree instanceof String) {
      println prefix + (is_tail ? "    " : "│   ") + "└──"  + newtree
    } else {
      print_tree(newtree, prefix + (is_tail ? "    " : "│   "))
    }
  }
}

def tree = [:]
explore(0, ['expr'], tree)
print_tree(tree, '')
