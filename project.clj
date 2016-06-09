(defproject usps-processor "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-simple "1.7.12"]
                 [turbovote.resource-config "0.2.0"]
                 [democracyworks/datomic-toolbox "2.0.1"
                  :exclusions [com.datomic/datomic-pro]]
                 [com.datomic/datomic-pro "0.9.5327"
                  :exclusions [org.slf4j/slf4j-nop]]
                 [clj-aws-s3 "0.3.10"]
                 [clj-time "0.11.0"]
                 [democracyworks/squishy "2.0.0"]
                 [org.clojure/data.csv "0.1.2"]
                 [turbovote.imbarcode "0.1.5"
                  :exclusions [org.clojure/clojure]]
                 [javax.servlet/servlet-api "2.5"]
                 [org.immutant/web "2.0.0"]
                 [riemann-clojure-client "0.3.2"]
                 [com.novemberain/langohr "3.1.0"]]
  :plugins [[lein-immutant "2.0.0"]]
  :profiles {:dev {:resource-paths ["dev-resources"]
                   :source-paths ["dev-src"]}
             :test {:resource-paths ["test-resources"]
                    :main usps-processor.core-test}
             :uberjar {:aot [usps-processor.importer]}
             :production {:resource-paths ["env-configs/usps-processor/production/resources"]}}
  :uberjar-name "usps-processor.jar"
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username :env
                                   :password :env}}
  :main usps-processor.core
  :aliases {"reset-db" ["run" "-m" "dev.db"]})
