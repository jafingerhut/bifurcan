load "common.gnu"

set ylabel "time (not time per element), in nanoseconds"

data = "../data/concat.csv"
set output dir."concat_time_all_rrb".ext
set title "concatenating lists, the fastest 4 libraries"

# Only include the data structures that appear to have O(log N) run
# time implementations of concatenation.  The others are so much
# slower for large vectors that auto-scaling the time axis for the
# largest ones makes the fast ones visually indistinguishable.  In
# this graph, we want to see any differences between the fast ones.

# The data in the .csv files is the run time divided by the vector
# size n, as calculated in the function write-out-csvs.  In order to
# plot the value of raw time on the time axis instead, we must
# multiply by n.

#lst="bifurcan.List java.ArrayList clojure.PersistentVector vavr.Vector scala.Vector paguro.RrbTree bifurcan.LinearList clojure.core.rrb-vector"
lst="paguro.RrbTree bifurcan.LinearList clojure.core.rrb-vector"

plot data using 1:(column("bifurcan.List")*$1) title "bifurcan.List", for [i in lst] '' using 1:(column(i)*$1) title i
