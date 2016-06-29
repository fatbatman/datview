(ns dat.view.router
  (:require [bidi.bidi :as bidi]
            [dat.view.settings :as settings]
            [dat.reactor.dispatcher :as dispatcher]
            [dat.view.routes :as routes]
            [datascript.core :as d]
            [reagent.ratom :refer-macros [reaction]]
            [goog.events]
            [dat.reactor :as reactor]
            [dat.view.utils :as utils])
  (:import [goog.history Html5History EventType]))


;(defprotocol Router
  ;(route))


;; Now we define how to instatiate the history object.

;; TODO Need to make a system component out of this thing

(defn make-history []
  (doto (Html5History.)
    (.setPathPrefix (str js/window.location.protocol
                         "//"
                         js/window.location.host))
    (.setUseFragment false)))


;; Now we set up our global history object. We use defonce so we can hot reload the code.

;; We should maybe be moving this into a constructor or something so that this state can be in the main app ns

(defn attach-history-handler!
  [history handler-fn]
  (doto history
    (goog.events/listen EventType.NAVIGATE
                        ;; wrap in a fn to allow live reloading
                        #(handler-fn %))
    (.setEnabled true)))


(defn update-route!
  [app]
  ;; If we put this in here, for the API we have to somenow let you add your own route customizations... XXX
  (dispatcher/dispatch! (:dispatcher app) [::path-change js/window.location.pathname]))

(reactor/register-handler ::path-change
  (fn [app db [_ new-path]]
    (reactor/resolve-to app db [[:dat.view.settings/update [::current-path new-path]]])))

(defn make-handler-fn
  [app]
  (fn [_]
    ;; Ideally, we'd be albe to extract the new route from the event...
    (update-route! app)))

;; Should rewrite these from app
(def current-route
  (memoize
    (fn [app]
      (reaction
        ;; Actually... :dat.sync/route should maybe just be its own ident...
        (bidi/match-route (utils/deref-or-value (::routes app)) @(settings/get-setting app ::current-path))))))


;; XXX Should probably handle this through a handler... but for now...
(defn set-route!
  [app {:as route :keys [handler route-params]}]
  (let [flattened-params (-> route-params seq flatten)
        path-for-route (apply bidi/path-for routes/routes handler flattened-params)]
    (if-not path-for-route
      (do
        (println "params:" (with-out-str (pr-str flattened-params)))
        (println "path:" path-for-route)
        (js/alert "Hit a bad route: " (pr-str route-params)))
      (.setToken (:history app)
                 path-for-route))))


