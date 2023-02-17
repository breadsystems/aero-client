(ns harrow.api
  (:require
    [clojure.string :as string]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [aero.core :as aero]))

(defn- http-route [form & {:as route}]
  (let [form (if (list? form) form (list form))
        [sym config & segments] form
        [config segments] (if (map? config)
                            [config segments]
                            [{} (cons config segments)])
        segments (vec (filter identity segments))]
    (merge {:route/name sym
            :route/config config
            :route/segments segments}
           route)))

(defmethod aero/reader 'http/get
  [_ _ route]
  (http-route route :method :get))

(defmethod aero/reader 'http/post
  [_ _ route]
  (http-route route :method :post))

(defmethod aero/reader 'http/delete
  [_ _ route]
  (http-route route :method :delete))

(defn- url [_ {:url/keys [join]} params]
  (clojure.string/join "/" (map #(get params %1 %1) join)))

(defn- req-key [ns-segments kname]
  (keyword
    (string/join
      "/"
      [(string/join "." (map name (filter identity ns-segments)))
       (name kname)])))

(defn- req-kv [api route subroute]
  (let [{:keys [base root-ns]} api
        req-name (:route/name subroute)
        req-ns (concat [root-ns] (filter string? route))
        ns-segments (filter identity req-ns)
        req-join (concat [base]
                         (butlast route)
                         (:route/segments subroute))

        k (keyword
            (string/join "/"
                         [(string/join "." (map name ns-segments))
                          (name req-name)]))
        v (assoc subroute :url/join req-join)]
    [k v]))

(defn- req-kvs [client route]
  (let [{:keys [base root-ns]} client
        subroutes (last route)
        route-name (first subroutes)
        named? (or (string? route-name) (keyword? route-name))
        [route-name subroutes] (if named?
                                 [route-name (rest subroutes)]
                                 [nil subroutes])]
    (map (partial req-kv client route) subroutes)))

(defn desc->client [{:keys [routes] :as desc}]
  (assoc desc :harrow/requests (into {} (mapcat #(req-kvs desc %) routes))))

(defn build-client [file]
  (desc->client (aero/read-config file)))

(defn build-request [client endpoint params]
  (let [route (get-in client [:harrow/requests endpoint])
        route-url (url client route params)
        {:harrow/keys [pass-thru-config pass-thru-params]
         :or {pass-thru-config [:headers]
              pass-thru-params [:query-params]}} client]
    (merge {:method (:method route)
            :url route-url}
           (select-keys client pass-thru-config)
           (select-keys params pass-thru-params))))

(defn request-keys [client]
  (-> client :harrow/requests keys))

(comment
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
