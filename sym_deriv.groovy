eval = [
  'var': { x            -> { _ -> x } },
  'mul': { l, r, x      -> { _ -> compile(l, x)() * compile(r, x)() } },
  'sin': { e, x         -> { _ -> Math.sin(compile(e, x)()) } },
  'pow': { base, exp, x -> { _ -> Math.pow(compile(base, x)(), compile(exp, x)()) } },
  'log': { b, x         -> { _ -> Math.log(compile(b, x))() }},
  'con': { it, x        -> { _ -> it } }
]


// should there even be primitive operations here? Like multiply etc
// also shouldnt call compile. Want this to be homomorphic, so it needs to return
// an original tree structure. 
rules = [
  'var': { x            -> { _ -> eval['con'](1, x)() } },
  'mul': { l, r, x      -> { _ -> derive(l, x)() * compile(r, x)() +
                                  compile(l, x)() * derive(r, x)() } },
  'sin': { e, x         -> { _ -> Math.cos(compile(e, x)()) * derive(e, x)() } },
  //'pow': long expression don't feel like writing it out now
  'log': { b, x         -> { _ -> derive(b, x)() / compile(b, x)() } },
  'con': { it, x        -> { _ -> 0 } }
]

def compile(expr, x='sentinel') {
    if ('sentinel' == x) {
        return { var -> compile(expr, var)() }
    }
    
    return eval[expr[0]](*expr.drop(1), x)
}

def derive(expr, x='sentinel') {
    if ('sentinel' == x) {
        return { var -> derive(expr, var)() }
    }
    
    return rules[expr[0]](*expr.drop(1), x)
}

// x * sin(x^2)
def expr = ['mul', 
               ['var'],
               ['sin',
                   ['pow', 
                       ['var'],
                       ['con', 2]
                   ]
               ]
           ]
       
compiled = compile(expr)
println compiled(2)

// x * sin(x)
expr = ['mul', ['sin', ['var']], ['var']]
derived = derive(expr)
println derived(1)
