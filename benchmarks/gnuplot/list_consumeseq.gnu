load "common.gnu"

set format y "%.1fns"

data = "../data/list_consumeseq.csv"
set output dir."list_consumeseq".ext
set title "iterating over lists using Clojure seq/next"

plot data using 1:2, for [i=3:21] '' using 1:i
