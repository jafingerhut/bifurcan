load "common.gnu"

set ylabel "time divided by log base 32 of number of elements, in nanoseconds"
data = "../data/concat.csv"
set output dir."concat_log".ext
set title "concatenating lists"

plot data using 1:(log($2*$1/log($1))), for [i=3:21] '' using 1:(log(column(i)*$1/log($1)))
