(ns harrow.impl.readers
  (:require
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
