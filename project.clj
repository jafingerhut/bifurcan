;; this is to allow the insecure `usethesource` repository
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

;; If you want to use a recent version of this dependency:

;; [org.scala-lang/scala-library "2.13.0"]

;; such as 2.12.0 or later, that appears to be incompatible with this
;; dependency:

;; [io.github.nicolasstucki/scala-rrb-vector_2.11 "0.1.1"]

;; because the scala-rrb-vector library was written in 2015-2016 time
;; frame, to be compatible with version 2.11.x of scala-library.  The
;; latest version of scala-library that I have done successful "smoke
;; testing" with scala-rrb-vector is 2.11.12, but the scala-rrb-vector
;; code itself mentions 2.11.6.  There appear to be changes made in
;; scala-library in 2.12.0 that scala-rrb-vector was never updated to
;; be compatible with.

;; In particular, version 2.12.0 and 2.13.0 of scala-library lead to
;; ClassNotFoundException exceptions when attempting to create an
;; instance of an RRBVector.

;; So it apears that two reasonable choices for running benchmarks
;; with this project are:

;; Option (1)

;; Test with latest scala-library, but not with scala-rrb-vector at
;; all, with these dependencies:

;; [org.scala-lang/scala-library "2.13.0"]

;; Option (2)

;; Test with older scala-library, and with scala-rrb-vector, using
;; these dependencies:

;; [org.scala-lang/scala-library "2.11.12"]
;; [io.github.nicolasstucki/scala-rrb-vector_2.11 "0.1.1"]


(defproject io.lacuna/bifurcan "0.2.0-alpha1"
  :java-source-paths ["src"]
  :dependencies []
  :test-selectors {:default   #(not
                                 (some #{:benchmark :stress}
                                   (cons (:tag %) (keys %))))
                   :benchmark :benchmark
                   :stress    :stress
                   :all       (constantly true)}
  :profiles {:travis {:jvm-opts ^:replace ["-server" "-Xmx1g"]}
             :bench  {:jvm-opts ^:replace ["-server" "-Xmx10g" #_"-XX:+UseParallelGC"]}
             :socket {:jvm-opts ["-Dclojure.server.repl={:port,50505,:accept,clojure.core.server/repl}"]}
             :dev    {:dependencies [;; for tests
                                     [org.clojure/clojure "1.8.0"]
                                     [org.clojure/test.check "0.9.0"]
                                     [criterium "0.4.5"]
                                     [potemkin "0.4.5"]
                                     [proteus "0.1.6"]
                                     [byte-streams "0.2.4"]
                                     [eftest "0.5.8"]
                                     [virgil "0.1.9"]
                                     [org.clojure/core.rrb-vector "0.1.0"]

                                     ;; for comparative benchmarks
                                     [io.usethesource/capsule "0.6.3"]
                                     [org.pcollections/pcollections "3.0.3"]
                                     [io.vavr/vavr "0.10.0"]
                                     ;; Option (1)
                                     [org.scala-lang/scala-library "2.13.0"]

                                     ;; Option (2)
                                     ;;[org.scala-lang/scala-library "2.11.12"]
                                     ;;[io.github.nicolasstucki/scala-rrb-vector_2.11 "0.1.1"]
                                     [org.functionaljava/functionaljava "4.8.1"]
                                     [org.eclipse.collections/eclipse-collections "9.2.0"]
                                     [org.organicdesign/Paguro "3.1.2"]]}}
  :aliases {"partest"   ["run" "-m" "bifurcan.run-tests"]
            "benchmark" ["run" "-m" "bifurcan.benchmark-test" "benchmark"]
            "help-benchmark" ["run" "-m" "bifurcan.benchmark-test" "help"]}
  :jvm-opts ^:replace ["-server"
                       "-XX:+UseG1GC"
                       "-XX:-OmitStackTraceInFastThrow"
                       "-ea:io.lacuna..."
                       "-Xmx4g"

                       #_"-XX:+UnlockDiagnosticVMOptions"
                       #_"-XX:+PrintAssembly"
                       #_"-XX:CompileCommand=print,io.lacuna.bifurcan.nodes.Util::mergeState"
                       #_"-XX:CompileCommand=dontinline,io.lacuna.bifurcan.nodes.Util::mergeState"
                       ]

  :repositories {"usethesource" "http://nexus.usethesource.io/content/repositories/public/"}

  ;; deployment
  :url "https://github.com/lacuna/bifurcan"
  :description "impure functional data structures"
  :license {:name "MIT License"}
  :javac-options ["-target" "1.8" "-source" "1.8"]
  :deploy-repositories {"releases"  {:url   "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
                                     :creds :gpg}
                        "snapshots" {:url   "https://oss.sonatype.org/content/repositories/snapshots/"
                                     :creds :gpg}}

  ;; Maven properties for the Maven God
  :scm {:url "git@github.com:lacuna/bifurcan.git"}
  :pom-addition [:developers [:developer
                              [:name "Zach Tellman"]
                              [:url "http://ideolalia.com"]
                              [:email "ztellman@gmail.com"]
                              [:timezone "-8"]]]
  :classifiers {:javadoc {:java-source-paths ^:replace []
                          :source-paths      ^:replace []
                          :resource-paths    ^:replace ["javadoc"]}
                :sources {:source-paths   ^:replace ["src"]
                          :resource-paths ^:replace []}})
