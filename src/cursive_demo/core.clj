(ns cursive-demo.core
  (:gen-class)
  (:require [cursive-demo.other :as other]
            [clj-http.client :as client]
            [com.climate.claypoole :as cp]
            [clojure.tools.logging :as log])

  )

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hi")
  )

;; Clojure 消除了可变变量，黑盒函数无法修改 my-object
(defn some-black-box-fn
  [my-object])

(comment
  (let [my-object {:name "value"}]
    (some-black-box-fn my-object)
    my-object))

;; 线程
(defn run-java-thread
  []
  (log/info "Before thread")
  (.start (new Thread (fn []
                        (Thread/sleep 1000)
                        (log/info "From thread"))))
  (log/info "After thread"))

;; future
(defn run-clojure-future
  []
  (log/info "Before future")
  (let [f (future
            (Thread/sleep 2000)
            (log/info "From future body")
            (log/info "from future")
            {:result "from future"})]
    (log/info "After future")
    (log/info "Result of future is " @f)))


;; parallel
(defn parallel-requests
  []
  (let [urls (->> (client/get "https://pokeapi.co/api/v2/pokemon-species?limit=100"
                              {:as :json})
                  :body
                  :results
                  (map :url))]
    (->> urls
         (map (fn [url]
                (future
                  (-> (client/get url {:as :json})
                      :body
                      (select-keys [:name :shape]))
                  )))
         (map deref)
         (doall))))
(comment
  (->> (client/get "https://pokeapi.co/api/v2/pokemon-species?limit=100" {:as :json})
       :body
       :results
       (map :url)
       )
  (let [url "https://pokeapi.co/api/v2/pokemon-species/200/"]
    (-> (client/get url {:as :json})
        :body
        (select-keys [:name :shape])
        )
    )
  )

(def counter 0)
(defn inc-counter-test
  []
  (alter-var-root #'counter (constantly 0))
  (let [f1 (future
             (dotimes [_ 1000]
               ;(Thread/sleep 1)
               (alter-var-root #'counter (constantly (inc counter)))
               )
             (log/info "f2 ends")
             ::f1)
        f2 (future
             (dotimes [_ 1000]
               ;(Thread/sleep 2)
               (alter-var-root #'counter (constantly (inc counter)))
               )
             (log/info "f2 ends")
             ::f2)]
    (and (deref f1) (deref f2))
    (log/info "Here!" @f1 @f2)
    counter))

(def counter-atom (atom 0))
(defn inc-counter-atom-test
  []
  (reset! counter-atom 0)
  (let [f1 (future
             (dotimes [_ 1000]
               (swap! counter-atom inc))
             ::f1)
        f2 (future
             (dotimes [_ 1000]
               (swap! counter-atom inc))
             ::f2)]
    (log/info "Here" @f1 @f2)
    (deref counter-atom)))


(def state-atom (atom []))
(swap! state-atom (fn [old-value arg1 arg2]
                    (println old-value arg1 arg2)
                    (conj old-value 1)) 10 20)


;; pmap 只适合 CPU 密集性程序，因为它根据核心数来创建线程
(comment
  (pmap (fn [input]) [1 2 3]))

(defn claypoole-parallel-requests
  []
  (let [urls (->> (client/get "https://pokeapi.co/api/v2/pokemon-species?limit=100"
                              {:as :json})
                  :body
                  :results
                  (map :url))]
    (->> urls
         (cp/pmap
           (cp/threadpool 100)
           (fn [url]
             (-> (client/get url {:as :json})
                 :body
                 (select-keys [:name :shape]))))
         (doall))))
(comment
  (let [urls (->> (client/get "https://pokeapi.co/api/v2/pokemon-species?limit=100" {:as :json})
                  :body
                  :results
                  (map :url))]
    urls))