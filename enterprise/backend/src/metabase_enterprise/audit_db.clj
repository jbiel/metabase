(ns metabase-enterprise.audit-db
  "This is here so we can try to require it and see whether or not EE code is on the classpath."
  (:require [clojure.pprint :as pprint]
            [metabase.db.connection :as mdb.connection]
            [metabase.models.database :refer [Database]]
            [metabase.util.log :as log]
            [metabase.util.malli :as mu]
            [toucan2.core :as t2]))

(defn- install-audit-db!
  "Creates the audit db, a clone of the app db used for auditing purposes."
  [app-db]
  (t2/insert! Database
              (cond-> {:is_audit     true
                       :name         "Audit Database"
                       :description  "Internal Audit DB used to power metabase analytics."
                       :engine       (:engine app-db)
                       :details      (:details app-db)
                       :is_full_sync (:is-full-sync? app-db false)
                       :is_on_demand (:is_on_demand app-db)
                       :cache_ttl    (:cache_ttl app-db)
                       ;; created by the system:
                       :creator_id   nil}
                (:created_at app-db) (assoc :created_at (:created_at app-db)))))

(mu/defn ensure-db-exists! :- [:enum ::installed ::replaced ::no-op]
  "Called on app startup to ensure the existance of the audit db in enterprise apps.

  Returns a keyword indicating what action was taken."
  []
  (let [audit-db (t2/select-one Database :is_audit true)
        app-db   (t2/select-one Database :id (:id mdb.connection/*application-db*))]
    (cond
      (nil? audit-db)
      (do (log/info "Audit DB does not exist, Installing...")
          (install-audit-db! app-db)
          ::installed)

      (not= (select-keys audit-db [:details :engine])
            (select-keys app-db [:details :engine]))
      (do (log/info "Audit DB does not match app-db, did something change?")
          (log/info (with-out-str
                      #_{:clj-kondo/ignore [:discouraged-var]}
                      (pprint/pprint [["App DB  " app-db]
                                      ["Old Audit DB" audit-db]])))
          (log/info "Deleting Old Audit DB...")
          (t2/delete! Database :is_audit true)
          (log/info "Installing Audit DB...")
          (install-audit-db! app-db)
          ::replaced)

      :else
      (do
        (log/info "Audit DB found.")
        ::no-op))))