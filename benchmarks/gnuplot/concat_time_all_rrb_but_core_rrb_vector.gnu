load "common.gnu"

set ylabel "time (not time per element), in nanoseconds"

data = "../data/concat.csv"
set output dir."concat_time_all_rrb_but_core_rrb_vector".ext
set title "concatenating lists, the fastest 3 libraries"

#lst="bifurcan.List java.ArrayList clojure.PersistentVector vavr.Vector scala.Vector paguro.RrbTree bifurcan.LinearList clojure.core.rrb-vector"
lst="paguro.RrbTree bifurcan.LinearList"

plot data using 1:(column("bifurcan.List")*$1) title "bifurcan.List", for [i in lst] '' using 1:(column(i)*$1) title i
