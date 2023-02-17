(ns harrow.api-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is are]]
    [harrow.api :as api]))

(deftest test-build-client
  (is (= {:com.example.api.thing/create!
          {:route/name :create!
           :route/config {}
           :route/segments []
           :method :post
           :url/join ["https://api.example.com" "thing"]}
          :com.example.api.thing/details
          {:route/name :details
           :route/config {}
           :route/segments [:thing/id]
           :method :get
           :url/join ["https://api.example.com" "thing" :thing/id]}
          :com.example.api.thing/update!
          {:route/name :update!
           :route/config {}
           :route/segments [:thing/id]
           :method :post
           :url/join ["https://api.example.com" "thing" :thing/id]}
          :com.example.api.thing/delete!
          {:route/name :delete!
           :route/config {}
           :route/segments [:thing/id]
           :method :delete
           :url/join ["https://api.example.com" "thing" :thing/id]}
          :com.example.api.hello/greet
          {:route/name :greet
           :route/config {}
           :route/segments [:greeting/name]
           :method :get
           :url/join ["https://api.example.com" "hello" :greeting/name]}}
         (-> "test-config.edn"
             io/resource
             api/build-client
             :harrow/requests))))

(deftest test-request-keys
  (let [client (-> "test-config.edn" io/resource api/build-client)]
    (is (= [:com.example.api.thing/create!
            :com.example.api.thing/details
            :com.example.api.thing/update!
            :com.example.api.thing/delete!
            :com.example.api.hello/greet]
           (api/request-keys client)))))

(deftest test-build-request
  (let [client (-> "test-config.edn" io/resource api/build-client)]
    (are
      [req args]
      (= req (apply api/build-request client args))

      {:url "https://api.example.com/thing"
       :method :post
       :query-params {:name "Thing One"}}
      [:com.example.api.thing/create! {:query-params {:name "Thing One"}}]

      {:url "https://api.example.com/thing/123"
       :method :get}
      [:com.example.api.thing/details {:thing/id 123}]

      {:url "https://api.example.com/thing/123"
       :method :post
       :query-params {:name "New Name"}}
      [:com.example.api.thing/update! {:thing/id 123
                                       :query-params {:name "New Name"}}]

      {:url "https://api.example.com/thing/123"
       :method :delete}
      [:com.example.api.thing/delete! {:thing/id 123}]

      {:url "https://api.example.com/hello/World"
       :method :get}
      [:com.example.api.hello/greet {:greeting/name "World"}]

      )))

(deftest test-build-request-pass-thru
  (are
    [req conf]
    (= req (let [[client-desc args] conf
                 client (api/desc->client client-desc)]
             (apply api/build-request client args)))

    {:method :get
     :url "https://api.example.com/my/endpoint/abc"
     :headers {"X-Test" "value"}
     :query-params {:x "X"}}
    [{:root-ns :com.example.api
      :base "https://api.example.com"
      :headers {"X-Test" "value"}
      :routes
      [["my" "endpoint" [{:route/name :foo
                          :route/config {}
                          :route/segments [:my-param]
                          :method :get}]]]}
     [:com.example.api.my.endpoint/foo {:my-param "abc"
                                        :query-params {:x "X"}}]]

    {:method :get
     :url "https://api.example.com/my/endpoint/abc"}
    [{:root-ns :com.example.api
      :base "https://api.example.com"
      :headers {"X-Test" "value"}
      :routes
      [["my" "endpoint" [{:route/name :foo
                          :route/config {}
                          :route/segments [:my-param]
                          :method :get}]]]
      ;; Override defaults to pass nothing through.
      :harrow/pass-thru-config #{}
      :harrow/pass-thru-params #{}}
     [:com.example.api.my.endpoint/foo {:my-param "abc"
                                        :query-params {:x "X"}}]]

    {:method :get
     :url "https://api.example.com/my/endpoint/abc"
     :headers {"X-Test" "value"}
     :query-params {:x "X"}
     :extra :EXTRA!
     :extra-param 123}
    [{:root-ns :com.example.api
      :base "https://api.example.com"
      :headers {"X-Test" "value"}
      :extra :EXTRA!
      :routes
      [["my" "endpoint" [{:route/name :foo
                          :route/config {}
                          :route/segments [:my-param]
                          :method :get}]]]
      ;; Override defaults to pass nothing through.
      :harrow/pass-thru-config #{:headers {"X-Test" "value"} :extra}
      :harrow/pass-thru-params #{:query-params :extra-param}}
     [:com.example.api.my.endpoint/foo {:my-param "abc"
                                        :extra-param 123
                                        :query-params {:x "X"}}]]

    ))

(comment
  (require '[kaocha.repl :as k])
  (k/run *ns*))
