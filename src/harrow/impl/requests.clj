(ns harrow.impl.requests
  (:require
    [clojure.string :as string]
    [clojure.edn :as edn]))

(defn url [_ {:url/keys [join]} params]
  (clojure.string/join "/" (map #(get params %1 %1) join)))

(defn- req-key [ns-segments kname]
  (keyword
    (string/join
      "/"
      [(string/join "." (map name (filter identity ns-segments)))
       (name kname)])))

(defn- req-kv [client route subroute]
  (let [{:harrow/keys [base root-ns]} client
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

(defn kvs [client route]
  (let [{:harrow/keys [base root-ns]} client
        subroutes (last route)
        route-name (first subroutes)
        named? (or (string? route-name) (keyword? route-name))
        [route-name subroutes] (if named?
                                 [route-name (rest subroutes)]
                                 [nil subroutes])]
    (map (partial req-kv client route) subroutes)))
