load "common.gnu"

set format y "%.1fns"

data = "../data/list_iterate.csv"
set output dir."list_iterate_all_but_core_rrb_vector".ext
set title "iterating over lists, the fastest 7 libraries"

#lst="bifurcan.List java.ArrayList clojure.PersistentVector vavr.Vector scala.Vector paguro.RrbTree bifurcan.LinearList clojure.core.rrb-vector"
lst="java.ArrayList clojure.PersistentVector vavr.Vector scala.Vector paguro.RrbTree bifurcan.LinearList"

plot data using 1:(column("bifurcan.List")) title "bifurcan.List", for [i in lst] '' using 1:(column(i)) title i
