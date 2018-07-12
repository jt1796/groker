def deriv = { f ->
    def e = 0.00001
    return { x ->
        return  (f(x + e) - f(x - e))/ (2*e)
    }
}

def integral = { f ->
    def e = 0.00001
    return { x ->
        return (0..(x/e)).collect { e * f(e * it) }.sum()
    }
}

def getTermMatrix = { w, h ->
    def matrix = (1..h).collect { (' ' * w).split('') }
    def fchars = ['$', 'o', 'x']
    def labels = []
    def graph
    graph = [
        'add_point': { x, y, c ->
            if (y >= h/2) { y = h/2; c = '-' }
            if (y <= -h/2) { y = -h/2 + 1; c = '_' }
            matrix[(int)(h/2 - y)][-1 * (int)(w/2 - x)] = c 
        },
        'add_function': { f, label, xspan, yspan ->
            def c = fchars.pop()
            labels << "$c : $label".toString()
            (-w/2..w/2 - 1).each { x -> graph['add_point'](x, f(x*xspan/w)*h/yspan, c) }
        },
        'print': {
            (0..h-1).each { matrix[it][(int)(w/2)] = '|' }
            (0..w-1).each { matrix[(int)(h/2)][it] = '-' }
            def max_w = labels.max { it.length() }.length()
            labels.eachWithIndex { l, i -> l.eachWithIndex { c, ci -> matrix[i + 1][w - max_w + ci] = c }  }
            println matrix.collect{ it.join() }.join('\n') 
        }
    ]
    return graph;
}

def matrix = getTermMatrix(400, 90)
def sin = { it == 0 ? -999 : 1 / it }

matrix['add_function'](sin, 'tan(x)', 10, 10)
matrix['add_function'](deriv(sin), 'd/dx sin(x)', 10, 10)
matrix['print']()
