(ns metabase.test.mock.toucanery
  "A document style database mocked for testing. This is a dynamic schema db with `:nested-fields`. Most notably meant
  to serve as a representation of a Mongo database."
  (:require
   [medley.core :as m]
   [metabase.driver :as driver]
   [metabase.test.mock.util :as mock.util]))

(def toucanery-tables
  {"transactions" {:name   "transactions"
                   :schema nil
                   :fields #{{:name                       "id"
                              :pk?                        true
                              :database-type              "SERIAL"
                              :base-type                  :type/Integer
                              :json-unfolding             false
                              :database-is-auto-increment true}
                             {:name                       "ts"
                              :database-type              "BIGINT"
                              :base-type                  :type/BigInteger
                              :effective-type             :type/DateTime
                              :coercion-strategy          :Coercion/UNIXMilliSeconds->DateTime
                              :json-unfolding             false
                              :database-is-auto-increment false}
                             {:name                       "toucan"
                              :database-type              "OBJECT"
                              :base-type                  :type/Dictionary
                              :json-unfolding             false
                              :database-is-auto-increment false
                              :nested-fields              #{{:name                       "name"
                                                             :database-type              "VARCHAR"
                                                             :base-type                  :type/Text
                                                             :json-unfolding             false
                                                             :database-is-auto-increment false}
                                                            {:name                       "details"
                                                             :database-type              "OBJECT"
                                                             :base-type                  :type/Dictionary
                                                             :json-unfolding             false
                                                             :database-is-auto-increment false
                                                             :nested-fields              #{{:name                       "age"
                                                                                            :database-type              "INT"
                                                                                            :database-is-auto-increment false
                                                                                            :json-unfolding             false
                                                                                            :base-type                  :type/Integer}
                                                                                           {:name                       "weight"
                                                                                            :database-type              "DECIMAL"
                                                                                            :database-is-auto-increment false
                                                                                            :json-unfolding             false
                                                                                            :semantic-type              :type/Category
                                                                                            :base-type                  :type/Decimal}}}}}
                             {:name           "buyer"
                              :database-type  "OBJECT"
                              :database-is-auto-increment false
                              :json-unfolding false
                              :base-type      :type/Dictionary
                              :nested-fields  #{{:name                       "name"
                                                 :database-type              "VARCHAR"
                                                 :json-unfolding             false
                                                 :base-type                  :type/Text
                                                 :database-is-auto-increment false}
                                                {:name                       "cc"
                                                 :database-type              "VARCHAR"
                                                 :json-unfolding             false
                                                 :base-type                  :type/Text
                                                 :database-is-auto-increment false}}}}}
   "employees"    {:name   "employees"
                   :schema nil
                   :fields #{{:name                       "id"
                              :database-type              "SERIAL"
                              :json-unfolding             false
                              :database-is-auto-increment true
                              :base-type                  :type/Integer}
                             {:name                       "name"
                              :database-type              "VARCHAR"
                              :json-unfolding             false
                              :database-is-auto-increment false
                              :base-type                  :type/Text}}}})

(driver/register! ::toucanery, :abstract? true)

(defmethod driver/describe-database ::toucanery
  [_ {:keys [exclude-tables]}]
  (let [tables (for [table (vals toucanery-tables)
                     :when (not (contains? exclude-tables (:name table)))]
                 (select-keys table [:schema :name]))]
    {:tables (set tables)}))

(defn- add-db-position
  [field position]
  (-> field
      (assoc :database-position position)
      (m/update-existing :nested-fields (partial (comp set map) #(add-db-position % position)))))

(defmethod driver/describe-table ::toucanery
  [_ _ table]
  (-> (get toucanery-tables (:name table))
      (update :fields (partial (comp set map-indexed) (fn [idx field]
                                                        (add-db-position field idx))))))

(defmethod driver/table-rows-seq ::toucanery
  [_ _ table]
  (when (= (:name table) "_metabase_metadata")
    [{:keypath "movies.filming.description", :value "If the movie is currently being filmed."}
     {:keypath "movies.description", :value "A cinematic adventure."}]))

(defmethod driver/supports? [::toucanery :nested-fields]
  [_ _]
  true)

(defmethod driver/mbql->native ::toucanery
  [_ query]
  query)

(defmethod driver/execute-reducible-query ::toucanery
  [_ query _ respond]
  (mock.util/mock-execute-reducible-query query respond))

(def toucanery-tables-and-fields
  [(merge mock.util/table-defaults
          {:name         "employees"
           :fields       [(merge mock.util/field-defaults
                                 {:name          "name"
                                  :display_name  "Name"
                                  :database_type "VARCHAR"
                                  :base_type     :type/Text
                                  :semantic_type :type/Name})
                          (merge mock.util/field-defaults
                                 {:name          "id"
                                  :display_name  "ID"
                                  :database_type "SERIAL"
                                  :base_type     :type/Integer
                                  :database_is_auto_increment true
                                  :semantic_type :type/PK})]
           :display_name "Employees"})
   (merge mock.util/table-defaults
          {:name         "transactions"
           :fields       [(merge mock.util/field-defaults
                                 {:name          "ts"
                                  :display_name  "Ts"
                                  :database_type "BIGINT"
                                  :base_type     :type/BigInteger
                                  :effective_type :type/DateTime
                                  :coercion_strategy :Coercion/UNIXMilliSeconds->DateTime})
                          (merge mock.util/field-defaults
                                 {:name          "id"
                                  :display_name  "ID"
                                  :database_type "SERIAL"
                                  :database_is_auto_increment true
                                  :base_type     :type/Integer})
                          (merge mock.util/field-defaults
                                 {:name          "buyer"
                                  :display_name  "Buyer"
                                  :database_type "OBJECT"
                                  :base_type     :type/Dictionary})
                          (merge mock.util/field-defaults
                                 {:name          "cc"
                                  :display_name  "Cc"
                                  :database_type "VARCHAR"
                                  :base_type     :type/Text
                                  :parent_id     true})
                          (merge mock.util/field-defaults
                                 {:name          "name"
                                  :display_name  "Name"
                                  :database_type "VARCHAR"
                                  :base_type     :type/Text
                                  :parent_id     true
                                  :semantic_type :type/Name})
                          (merge mock.util/field-defaults
                                 {:name          "age"
                                  :display_name  "Age"
                                  :database_type "INT"
                                  :base_type     :type/Integer
                                  :parent_id     true})
                          (merge mock.util/field-defaults
                                 {:name          "details"
                                  :display_name  "Details"
                                  :database_type "OBJECT"
                                  :base_type     :type/Dictionary
                                  :parent_id     true})
                          (merge mock.util/field-defaults
                                 {:name          "name"
                                  :display_name  "Name"
                                  :database_type "VARCHAR"
                                  :base_type     :type/Text
                                  :parent_id     true
                                  :semantic_type :type/Name})
                          (merge mock.util/field-defaults
                                 {:name          "toucan"
                                  :display_name  "Toucan"
                                  :database_type "OBJECT"
                                  :base_type     :type/Dictionary})
                          (merge mock.util/field-defaults
                                 {:name          "weight"
                                  :display_name  "Weight"
                                  :database_type "DECIMAL"
                                  :base_type     :type/Decimal
                                  :parent_id     true
                                  :semantic_type :type/Category})]
           :display_name "Transactions"})])
