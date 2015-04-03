(ns usps-processor.core
  (:require [usps-processor.api :as api]
            [usps-processor.importer :as importer]
            [immutant.daemons :refer [singleton-daemon]]
            [immutant.web :as web]
            [clojure.tools.logging :refer [info]])
  (:gen-class))

(def messages-future (atom nil))
(def api-server (atom nil))

(defn start-importer []
  (swap! messages-future (fn [mf]
                           (when mf (future-cancel mf))
                           (importer/-main))))

(defn stop-importer []
  (swap! messages-future #(when %
                            (future-cancel %)
                            nil)))

(defn start-web-server []
  (swap! api-server (fn [as]
                      (when as (web/stop as))
                      (api/-main))))

(defn stop-web-server []
  (swap! api-server #(when %
                       (web/stop %))))

(defn -main [& args]
  (info "Starting immutant daemon for importer")
  (singleton-daemon "usps-processor.importer" start-importer stop-importer)
  (info "Starting api server")
  (api/-main))
