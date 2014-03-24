(ns usps-processor.s3
  (:require [aws.sdk.s3 :as s3]
            [clojure.java.io :as io]
            [turbovote.resource-config :refer [config]]))

(defn reader-from-s3 [key]
  (-> (s3/get-object (config :s3 :creds)
                     (config :s3 :bucket)
                     key)
      :content
      io/reader))
