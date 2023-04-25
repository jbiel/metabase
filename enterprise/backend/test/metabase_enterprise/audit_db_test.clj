(ns metabase-enterprise.audit-db-test
  (:require [clojure.test :refer [deftest is]]
            [metabase-enterprise.audit-db :as audit-db]
            [metabase.models.database :refer [Database]]
            [toucan2.core :as t2]))

(defmacro with-delete-audit-db [& body]
  `(let [original-audit-db# (t2/select-one Database :is_audit true)]
     (try
       (t2/delete! Database :is_audit true)
       ~@body
       (finally
         (t2/delete! Database :is_audit true)
         (when original-audit-db#
           (#'audit-db/install-audit-db! original-audit-db#))))))

(deftest modified-audit-db-engine-is-replaced-test
  (with-delete-audit-db
    (let [_ (audit-db/ensure-db-exists!)
          audit-db (t2/select-one Database :is_audit true)
          _ (t2/update! Database :id (:id audit-db) {:engine "postgres"})]
      (is (= ::audit-db/replaced
             (audit-db/ensure-db-exists!))))))

(deftest modified-audit-db-details-replaced-test
  (with-delete-audit-db
    (let [_ (audit-db/ensure-db-exists!)
          audit-db (t2/select-one Database :is_audit true)
          _ (t2/update! Database :id (:id audit-db)
                        {:details {:db
                                   (str "file:/someplace/new/sample-database.db;"
                                        "USER=NEW_USER;PASSWORD=correcthorsebatterystaple")}})]
      (is (= ::audit-db/replaced
             (audit-db/ensure-db-exists!))))))

(deftest unmodified-audit-db-is-left-alone
  (with-delete-audit-db
    (is (= ::audit-db/installed
           (audit-db/ensure-db-exists!)))
    (is (= ::audit-db/no-op
           (audit-db/ensure-db-exists!)))))