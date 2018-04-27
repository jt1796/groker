def deal(n) {
  ranks = '23456789TJQKA'.findAll(".")
  suits = 'DCHS'.findAll(".")
  cards = ranks.collect { r -> suits.collect { s -> "$r$s"} }.flatten()
  Collections.shuffle(cards)
  return cards.collate(n)
}

def kind(ranks, n) {
  counts(ranks).contains(n)
}

def counts(items) {
  return items.toUnique().collect { u -> items.count { it == u } }
}

def straight(ranks) {
  if (ranks.toUnique() != ranks) {
    return false
  }
  mod_ranks = ranks.collect { (it == 14) ? 1 : it }
  return 4 == mod_ranks.findAll { r -> mod_ranks.contains(r - 1) }.size() ||
         4 == ranks.findAll { r -> ranks.contains(r - 1) }.size()
}

def flush(suits) {
  return kind(suits, 5)
}

def hand_type(hand) {
  ranks = hand.collect { it[0] }.collect { '--23456789TJQKA'.indexOf(it) }
  suits = hand.collect { it[1] }
  if (kind(ranks, 5)) {
    return [0, "five of a kind"] // not even possible without a joker
  } else if (flush(suits) && straight(ranks)) {
    return [1, "straight flush"]
  } else if (kind(ranks, 4)) {
    return [2, "four of a kind"]
  } else if (kind(ranks, 3) && kind(ranks, 2)) {
    return [3, "full house"]
  } else if (flush(suits)) {
    return [4, "flush"]
  } else if (straight(ranks)) {
    return [5, "straight"]
  } else if (kind(ranks, 3)) {
    return [6, "three of a kind"]
  } else if (counts(ranks).findAll {it == 2}.size() == 2) {
    return [7, "two pair"]
  } else if (kind(ranks, 2)) {
    return [8, "one pair"]
  } else {
    return [9, "high card"]
  }
}

def print_probabilities() {
  types = (0..9).collectEntries { [(it): 0.0] }
  trials = 500000
  (0..trials).each {
    hand = deal(5)[0]
    type = hand_type(hand)[0]
    types[type]++
  }
  println types.collectEntries { type, count -> [type, (count/trials * 100) + '%'] }
}

print_probabilities()
