load "common.gnu"

data = "../data/list_construct.csv"
set output dir."list_construct_all_but_vavr".ext
set title "constructing lists, the fastest 7 libraries"

#lst="bifurcan.List java.ArrayList clojure.PersistentVector vavr.Vector scala.Vector paguro.RrbTree bifurcan.LinearList clojure.core.rrb-vector"
lst="java.ArrayList clojure.PersistentVector scala.Vector paguro.RrbTree bifurcan.LinearList clojure.core.rrb-vector"

plot data using 1:(column("bifurcan.List")) title "bifurcan.List", for [i in lst] '' using 1:(column(i)) title i
