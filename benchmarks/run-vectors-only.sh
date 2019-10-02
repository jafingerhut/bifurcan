#! /bin/bash

INSTALL_DIR=$(dirname "$BASH_SOURCE")
#echo "BASH_SOURCE=:${BASH_SOURCE}:"
#echo "INSTALL_DIR=:${INSTALL_DIR}:"
#exit 0

# Run benchmarks only on classes that implement vectors / lists.  See
# 'def lists' in file test/bifurcan/benchmark_test.clj

echo "Running benchmarks only for the classes that are like Clojure vectors."
echo "When finished, result data will be stored in benchmarks/data directory."
echo "With the parameters used in this script, running the benchmarks will"
echo "take about 60 minutes."
echo ""
set -x
time lein benchmark 1000000 4 bifurcan.List java.ArrayList clojure.PersistentVector vavr.Vector scala.Vector paguro.RrbTree bifurcan.LinearList clojure.core.rrb-vector
set +x
cd "${INSTALL_DIR}"
cd gnuplot
for x in concat*.gnu list*.gnu
do
    set -x
    gnuplot $x
    set +x
done
echo ""
echo "Ran gnuplot to generate only the graphs related to vectors."
echo "It is normal for lines like the one below to appear above from running gnuplot:"
echo ""
echo "   \"list_lookup.gnu\" line 7: warning: Skipping data file with no valid points"
echo ""
echo "The files with .gnu suffixes listed below are the gnuplot control/program"
echo "files."
echo ""
ls concat*.gnu list*.gnu
echo ""
echo "The corresponding images are in the benchmarks/images directory."
