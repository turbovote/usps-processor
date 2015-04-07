(ns usps-processor.importer
  (:require [usps-processor.s3 :as s3]
            [usps-processor.parse :as parse]
            [usps-processor.db :as db]
            [usps-processor.queue :as queue]
            [democracyworks.squishy :as sqs]
            [clojure.tools.logging :refer [info]]
            [turbovote.resource-config :refer [config]]
            [turbovote.datomic-toolbox :as d]
            [datomic.api :as datomic]
            [riemann.client :as riemann]
            [clojure.edn :as edn])
  (:gen-class))

(def riemann-client
  (memoize (fn [] (riemann/udp-client :host (config :riemann :host)
                                      :port (config :riemann :port)))))

(defn send-event [metric description]
  (when (config :riemann :host)
    (riemann/send-event (riemann-client)
                        {:service "usps-processor scans" :metric metric
                         :tags ["usps-processor"]
                         :description description}
                        false)))

(defn process-file [message]
  (let [{:keys [bucket filename]} (edn/read-string (:body message))]
    (info "Processing" bucket "/" filename)
    (let [scans (parse/parse (s3/reader-from-s3 bucket filename))
          scan-count (count scans)
          stored-scans (db/store-scans scans)]
      (queue/publish-scans stored-scans)
      (send-event scan-count (str "Processed " scan-count " scans from " bucket))
      (info "Processed" bucket "/" filename))))

(defn -main [& args]
  (info "Starting up...")
  (d/initialize)
  (queue/initialize)
  (let [messages-future (sqs/consume-messages (sqs/client) process-file)]
    (info "Started")
    messages-future))
