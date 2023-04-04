(ns metabase.api.model-index
  (:require
   [clojure.set :as set]
   [compojure.core :refer [POST]]
   [metabase.api.common :as api]
   [metabase.models.card :refer [Card]]
   [metabase.models.model-index :as model-index :refer [ModelIndex]]
   [metabase.sync.schedules :as sync.schedules]
   [metabase.util.cron :as u.cron]
   [metabase.util.i18n :refer [tru]]
   [metabase.util.malli.schema :as ms]
   [toucan2.core :as t2]))

(defn default-schedule
  "Default sync schedule for indexed values. Defaults to randomly once a day."
  []
  (u.cron/schedule-map->cron-string (sync.schedules/randomly-once-a-day)))

(api/defendpoint POST "/"
  [:as {{:keys [model_id pk_ref value_ref] :as _model-index} :body}]
  {model_id  ms/PositiveInt
   pk_ref    any?
   value_ref any?}
  (let [model      (api/read-check Card model_id)
        field_refs (into #{} (map :field_ref) (:result_metadata model))]
    (when-let [missing (seq (set/difference (into #{} (map model-index/normalize-field-ref) [pk_ref value_ref])
                                            field_refs))]
      (throw (ex-info (tru "Unrecognized fields to index")
                      {:missing missing
                       :present field_refs})))
    ;; todo: do we care if there's already an index on that model?
    (t2/insert! ModelIndex {:model_id   model_id
                            ;; todo: sanitize these?
                            :pk_ref     pk_ref
                            :value_ref  value_ref
                            :generation 0
                            :schedule   (default-schedule)
                            :created_by api/*current-user-id*})))

(api/defendpoint DELETE "/:id"
  [id]
  (let [model-index (api/read-check ModelIndex id)]
    (api/read-check Card (:model_id model-index))
    (t2/delete! ModelIndex id)))
