(ns rental.schema
  (:require
    [clojure.edn :refer [read-string]]
    [clojure.pprint :refer [pprint]]
    [clojure.tools.logging :as log :refer [info]]
    [datomic.api :as d]
    [carica.core :as cc]
    [cemerick.friend.credentials :as creds]
    [hiccup.page :as h :refer [html5]]
    [hiccup.element :as h-e :refer [link-to unordered-list]]
    [rental.views.layout :as layout]
  )
)

(def insert-admin '({:db/id #db/id[:db.part/user] :rental.schema/usertype :rental.schema.usertype/admin :rental.schema/username "admin" :rental.schema/password "password"}))

(def uri (cc/config :db-url))

; Attribute map added as per https://groups.google.com/forum/#!topic/datomic/aPtVB1ntqIQ
; Otherwise clojure.edn/read-string throws "No reader function for tag db/id" message
; Regular read-string works just fine witout attributes
(def schema-tx (read-string {:readers *data-readers*} (slurp (first (cc/resources (cc/config :config)))))
(def setup-data-tx (read-string {:readers *data-readers*} (slurp (first (cc/resources (cc/config :setup-data))))))
(def setup-data-encrypted-passwords-tx (map #(assoc % :rental.schema/password (creds/hash-bcrypt (:rental.schema/password %))) setup-data-tx))
  
(def menu
  [
   (h-e/link-to "/schema/create" "Create database")
   (h-e/link-to "/schema/delete" "Delete database")
  ]
)

(defn start []
  (log/info "Starting db " uri "...")
)

(defn stop []
  (log/info "Stopping db" uri "...")
)

(defn maintenance []
 (log/info "Schema maintenance")
 (layout/common 
   [:h1 "Schema maintenance"] 
   [:br]
   "URI=" uri
   [:br]
   "Schema file=" (cc/config :db-schema)
   [:br]
   (h-e/unordered-list menu)
 )
)

(defn create-database []
  (if (d/create-database uri)
    (do
      (log/info "database" uri "created.")
      (log/info "Adding schema:" schema-tx)
      (log/info "Connection:" (d/connect uri))
      @(d/transact (d/connect uri) schema-tx)
      (log/info "Schema added.")
      @(d/transact (d/connect uri) setup-data-encrypted-passwords-tx)
      (log/info "Setup data added.")
    )
    (log/info "database" uri "already exists.")
  )
  (ring.util.response/redirect "/schema")
)

(defn delete-database []
  (if (d/delete-database uri)
    (log/info "database" uri "deleted.")
    (log/info "database" uri "was not deleted.")
  )
  (ring.util.response/redirect "/schema")
)
