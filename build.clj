(ns build
  (:require
    [clojure.tools.build.api :as b]
    [deps-deploy.deps-deploy :as dd]))

(def minor-version "0.0")

(def patch-version (format "%s.%s" minor-version (b/git-count-revs nil)))
(def jar-file (format "target/harrow-%s.jar" patch-version))

(defn tag [_]
  (print (str "v" patch-version)))

(defn clean [_]
  (println "Cleaning target...")
  (b/delete {:path "target"}))

(defn jar [_]
  (println "Building jar...")
  (b/write-pom {:class-dir "target/classes"
                :lib 'systems.bread/harrow
                :version patch-version
                :basis (b/create-basis {:project "deps.edn"})
                :scm {:tag (str "v" patch-version)
                      :url "https://github.com/breadsystems/harrow"}
                :src-dirs ["src"]})
  (b/copy-dir {:src-dirs ["src"]
               :target-dir "target/classes"})
  (b/jar {:class-dir "target/classes"
          :jar-file jar-file}))

(defn deploy [_]
  (println "Deploying to Clojars...")
  #_
  (dd/deploy {:installer :remote
              :artifact jar-file
              :pom-file (b/pom-path {:lib 'systems.bread/harrow
                                     :class-dir "target/classes"})}))
