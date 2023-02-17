(ns harrow.api
  (:require
    [harrow.impl.readers]
    [harrow.impl.requests :as req]
    [aero.core :as aero]))

(defn desc->client
  "Takes a client description map and returns a client"
  [{:keys [routes] :as desc}]
  (as-> desc $
    (clojure.set/rename-keys $ {:root-ns :harrow/root-ns
                                :base :harrow/base
                                :routes :harrow/routes})
    (assoc $ :harrow/requests (into {} (mapcat #(req/kvs $ %) routes)))))

(defn build-client
  "Takes a filename or resource and returns a client"
  [file]
  (desc->client (aero/read-config file)))

(defn build-request
  "Returns an HTTP Kit-style request map for endpoint with the given params"
  [client endpoint params]
  (let [route (get-in client [:harrow/requests endpoint])
        route-url (req/url client route params)
        {:harrow/keys [pass-thru-config pass-thru-params]
         :or {pass-thru-config [:headers]
              pass-thru-params [:query-params]}} client]
    (merge {:method (:method route)
            :url route-url}
           (select-keys client pass-thru-config)
           (select-keys params pass-thru-params))))

(defn request-keys
  "Returns the request keys in the given client spec as a collection"
  [client]
  (-> client :harrow/requests keys))

(comment
  (require '[clojure.java.io :as io])
  (def $client (build-client (io/resource "client.edn")))

  (aero/read-config (io/resource "client.edn"))
  (request-keys $client)
  (get-in $client [:harrow/requests :northflank.v1.projects.secrets/get])
  (build-request $client :northflank.v1.projects.secrets/list {:project/id "xyz"})
  (build-request $client :northflank.v1.projects.secrets/create!
                 {:project/id "xyz"
                  :query-params {:data {:x "X"}}})
  (build-request $client :northflank.v1.projects.secrets/get {:project/id "qwerty"})
  (build-request $client :northflank.v1.projects.secrets/delete! {:project/id "xyz"}))
