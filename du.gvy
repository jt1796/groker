#! /usr/bin/groovy

def walk_fs(parent, code) {
  File file = new File(parent)
  if (file.isDirectory()) {
    children = []
    file.eachFile() { children << it }
    children = children.findAll { it != "." || it != ".." }
    children.each { walk_fs(parent + '/' + it.getName(), code) }
  } else {
    code(parent)
  }
}

def files = []
walk_fs(args[0], { files << [it, (int) (new File(it).length() / 1000000)] })

def buckets = [:].withDefault { 0 }
files.each { buckets[it[0].tokenize('/').take(args[1].toInteger()).drop(1).join('/') ] += it[1] }
buckets.sort { it.value }.each { k, v -> println "$k".padRight(30) + "  --  $v MB".center(10) }
