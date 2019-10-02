load "common.gnu"

set ylabel "time divided by log base 32 of number of elements, in nanoseconds"
data = "../data/concat.csv"
set output dir."concat_log".ext
set title "concatenating lists"

# 1000000   size
# 1.1E-4    bifurcan.List
# 1.53E-4   bifurcan.LinearList
# 5.11E-4   paguro.RrbTree
# 0.044761  clojure.core.rrb-vector
# 0.955233  scala.Vector
# 5.214885  vavr.Vector
# 5.816767  java.ArrayList
# 19.54509  clojure.PersistentVector

# Only include the data structures that appear to have O(log N) run
# time implementations of concatenation.  The others are so much
# slower for large vectors that auto-scaling the time axis for the
# largest ones makes the fast ones visually indistinguishable.  In
# this graph, we want to see any differences between the fast ones.

# The data in the .csv files is the run time divided by the vector
# size n, as calculated in the function write-out-csvs.  In order to
# plot the values "time / log_32(n)" on the time axis instead, we must
# multiply by n, then divide by log_32(n).

#lst="bifurcan.List java.ArrayList clojure.PersistentVector vavr.Vector scala.Vector paguro.RrbTree bifurcan.LinearList clojure.core.rrb-vector"
lst="bifurcan.List paguro.RrbTree bifurcan.LinearList clojure.core.rrb-vector"

plot data using 1:($2*$1/log($1)), for [i in lst] '' using 1:(column(i)*$1/log($1)) title i
