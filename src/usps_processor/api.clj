(ns usps-processor.api
  (:require [usps-processor.mailing :as mailing]
            [usps-processor.zip-lookup :refer [zipcode->city-state]]
            [turbovote.datomic-toolbox :as d]
            [compojure.core :refer [defroutes GET]]
            [compojure.handler :refer [site]]
            [immutant.web :refer [run]]
            [turbovote.resource-config :refer [config]]
            [clojure.set :as set]
            [clojure.tools.logging :refer [info]]
            [democracyworks.squishy.data-readers])
  (:gen-class))

(def param->query-key
  {:serial-6 :mailing/serial-number-6
   :mailer-9 :mailing/mailer-id-9
   :serial-9 :mailing/serial-number-9
   :mailer-6 :mailing/mailer-id-6
   :customer-number :mailing/customer-number
   :routing-code :mailing/routing-code})

(defn params->mailing-constraints
  [params]
  (-> params
      (select-keys (keys param->query-key))
      (set/rename-keys param->query-key)))

(defn edn-response [body]
  {:status 200
   :headers {"Content-Type" "application/edn"}
   :body (pr-str body)})

(defn attach-facility-city-state [scan]
  (assoc scan
    :scan/facility-city-state
    (some-> scan
            :scan/facility-zip
            (Integer/parseInt)
            zipcode->city-state)))

(defn render-scan
  [scan]
  (-> scan
      (select-keys [:scan/time :scan/barcode :scan/facility-zip
                    :scan/operation-code :scan/service])
      attach-facility-city-state))

(defn lookup-mailings [req]
  (let [db (d/db)
        mailing-constraints (-> req :params params->mailing-constraints)
        mailings (d/match-entities db mailing-constraints)]
    mailings))

(defn on-single-match [matches match->body]
  (case (count matches)
      1 (-> matches
            first
            match->body
            edn-response)
      0 (-> "Not found"
          edn-response
          (assoc :status 404))
      (-> "Multiple matches found"
          edn-response
          (assoc :status 422))))

(defn latest-scan [req]
  (on-single-match (lookup-mailings req)
                   (comp render-scan mailing/latest-scan)))

(defn all-scans [req]
  (on-single-match (lookup-mailings req)
                   (comp (partial map render-scan) mailing/all-scans)))

(defroutes app
  (GET "/ping" [] "pong!")
  (GET "/latest-scan" [] latest-scan)
  (GET "/all-scans" [] all-scans))

(defn -main [& args]
  (let [port (config :api :port)]
    (info "API Server starting up on port" port)
    (run (site app) {:port port})))
