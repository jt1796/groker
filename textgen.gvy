def load_text_arr() {
    return new File('textgen.sample.txt').getText()
        .replaceAll('\\.', ' . ')
        .replaceAll(',', ' , ')
        .replaceAll('\'', '')
        .split(/\s/)
        .findAll({ word -> word.equals('.') || word.matches(/^[a-zA-Z]+$/) })
        .collect({ word -> word.toLowerCase() })
}


// prevword, word -> P(word)
def second_prob_map(word_arr) {
    def map = [:].withDefault({ 0 })
    def lead_count = [:].withDefault({ 0 })
    def prev = word_arr.head()
    for (String word : word_arr.tail()) {
        lead_count[prev] += 1
        map[[prev, word]] += 1
        prev = word
    }

    for (Object pair : map.keySet()) {
        def lead = lead_count[pair[0]]
        map[pair] /= lead
    }

    return map
}

def next_word(cur, map) {
    def possib = [:].withDefault({0})
    for (Object pairs : map.keySet()) {
        if (pairs[0] == cur) {
            possib[pairs[1]] = map[pairs]
        }
    }

    return first_word(possib.keySet().toList(), possib)
}

//   word -> P(word)
def prob_map(word_arr) {
    def map = [:].withDefault({ 0 })
    for (String word : word_arr) {
        map[word] += 1 / word_arr.size()
    }

    return map
}

def first_word(words, map) {
    for (String word : words) {
        if (Math.random() <= map[word] && map[word] != '.') {
            return word
        }
    }

    return first_word(words, map)
}

def shuffle_map(map) {
    def newmap = [:].withDefault({0})
    def toShuffle  = map.keySet().toList()
    Collections.shuffle(toShuffle)
    for (Object key : toShuffle) {
        newmap[key] = map[key]
    }

    return newmap
}

def arr = load_text_arr()
def uniq = arr.unique(false)
Collections.shuffle(uniq)
def map = shuffle_map(prob_map(arr))
def pairmap = shuffle_map(second_prob_map(arr))

def cur = first_word(uniq, map)
def toCap = true
for (int i = 0; i < 150; i++) {
    if (toCap || cur == 'i') {
        print cur.capitalize()
    } else {
        print cur
    }
    if (cur == '.') {
        toCap = true
        if (i > 60) {
            break
        }
    } else {
        toCap = false
    }
    cur = next_word(cur, pairmap)
    if (cur != '.') {
        print ' '
    }
}

println ''
