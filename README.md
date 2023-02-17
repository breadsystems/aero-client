# Harrow

![Build status](https://github.com/breadsystems/harrow/actions/workflows/test.yml/badge.svg)
[![Clojars Project](https://img.shields.io/clojars/v/systems.bread/harrow.svg)](https://clojars.org/systems.bread/harrow)

Define simple HTTP API clients with [Aero](https://github.com/juxt/aero).

## Usage

Define your client using a simple Aero (EDN) config:

```clj
; client.edn
{:ns :com.example.api.v1
 :base "https://api.example.com/v1"
 :routes
 [["thing" [;; GET /thing/:id
            #http/get (:get-details :thing/id)
            ;; POST /thing
            #http/post :create!
            ;; DELETE /thing/:id
            #http/delete (:delete! :thing/id)]]
  ["hello" [;; GET /hello/:name
            #http/get (:greet :greeting/name)]]]}
```

Create a client description from your config:

```clj
(ns my-app
  (:require
    [org.httpkit.client :as http]
    [harrow.api :as harrow]))

(def api (harrow/build-client))

(harrow/request-keys api)
;; => (:com.example.api.v1.thing/get-details
;;     :com.example.api.v1.thing/create!
;;     :com.example.api.v1.thing/delete!
;;     :com.example.api.v1.hello/greet)
```

Build a complete HTTP request for a specific endpoint:

```clj
(harrow/build-request
  api :com.example.api.v1.hello/greet
  {:greeting/name "World"})
;; => {:method :get
;;     :url "https://api.example.com/v1/hello/World"}

;; The special :query-params key is pass-through, as are all http-kit options:
;; http://http-kit.github.io/client.html
(harrow/build-request
  api :com.example.api.v1.thing/create!
  {:query-params {:data {:name "Thing One"}}})
;; => {:method :post
;;     :url "https://api.example.com/v1/thing"
;;     :query-params {:data {:name "Thing One"}}}
```

Call some endpoints!

```clj
;; Assume the /hello endpoint implements a simple "Hello, {name}!" program...
(http/request (harrow/build-request api :com.example.api.v1.hello/greet
                                    {:greeting/name "World"}))
;; => "Hello, World!"

;; Create our Thing One
(http/request (harrow/build-request api :com.example.api.v1.thing/create!
                                    {:query-params
                                     {:data {:name "Thing One"}}}))
;; => {:id 123}

;; Get details about Thing One
(http/request (harrow/build-request api :com.example.api.v1.thing/details
                                    {:thing/id 123}))
;; => {:id 123 :name "Thing One" :created-at "2022-12-23 14:21:15"}

;; Delete Thing One
(http/request (harrow/build-request api :com.example.api.v1.thing/delete!
                                    {:thing/id 123}))
;; => {:deleted true}
```
