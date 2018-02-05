(ns xapi-schema.spec
  (:require
   [xapi-schema.schemata.predicates :refer [re-pred]]
   [xapi-schema.schemata.regex :refer [LanguageTagRegEx
                                       OpenIdRegEx
                                       AbsoluteIRIRegEx
                                       MailToIRIRegEx
                                       UuidRegEx
                                       TimestampRegEx
                                       xAPIVersionRegEx
                                       DurationRegEx
                                       Base64RegEx
                                       Sha1RegEx]]
   [clojure.set :refer [intersection
                        difference]]
   [clojure.walk :refer [stringify-keys]]
   [clojure.spec.alpha :as s #?@(:cljs [:include-macros true])]))

(defn conform-ns-map [map-ns string-map]
  (try (reduce-kv (fn [m k v]
                    (assoc m (if (string? k)
                               (keyword map-ns k)
                               k) v))
                  {}
                  string-map)
       (catch #?(:clj Exception
                 :cljs js/Error) e
         ::s/invalid)))

(defn unform-ns-map [keyword-map]
  (try (reduce-kv (fn [m k v]
                    (assoc m (if (qualified-keyword? k)
                               (name k)
                               k) v))
                  {}
                  keyword-map)
       (catch #?(:clj Exception
                 :cljs js/Error) e
         ::s/invalid)))

(defn map-ns-conformer
  [map-ns]
  (s/conformer
   (partial conform-ns-map map-ns)
   unform-ns-map))

;; primitives - useful to hook into for generation
(s/def ::string-not-empty
  (s/and string?
         (complement empty?)))

;; Leaves

(s/def ::language-tag
  (s/and ::string-not-empty
         (partial re-find LanguageTagRegEx)))

(s/def ::language-map
  (s/map-of ::language-tag
            ::string-not-empty
            :gen-max 3
            :min-count 1))

(s/def ::iri
  (s/and string?
         (partial re-find AbsoluteIRIRegEx)))

(s/def ::mailto-iri
  (s/and string?
         (partial re-find MailToIRIRegEx)))

(s/def ::irl
  (s/and string?
         (partial re-find AbsoluteIRIRegEx)))

(s/def ::any-json
  (s/or :scalar
        (s/or :string
              string?
              :number
              (s/or :double
                    double?
                    :int
                    int?))
        :coll
        (s/or :map
              (s/map-of
               string?
               ::any-json
               :gen-max 4)
              :vector
              (s/coll-of
               ::any-json
               :kind vector?
               :into []
               :gen-max 4))))

(s/def ::extensions
  (s/map-of ::iri
            ::any-json
            :min-count 1)) ;; TODO: spec for any valid json

(s/def ::openid
  (s/and string?
         (partial re-find OpenIdRegEx)))

(s/def ::uuid
  (s/and string?
         (partial re-find UuidRegEx)))

(s/def ::timestamp
  (s/and string?
         (partial re-find TimestampRegEx)))

(s/def ::duration
  (s/and string?
         (partial re-find DurationRegEx)))

(s/def ::version
  (s/and string?
         (partial re-find xAPIVersionRegEx)))

(s/def ::sha2
  (s/and string?
         (partial re-find Base64RegEx)))

(s/def ::sha1sum
  (s/and string?
         (partial re-find Sha1RegEx)))

;; Activity Definition

(s/def :interaction-component/id
  ::string-not-empty)

(s/def :interaction-component/description
  ::language-map)

(s/def ::interaction-component*
  (s/keys :req [:interaction-component/id]
          :opt [:interaction-component/description]))

(s/def ::interaction-component
  (s/and (map-ns-conformer "interaction-component")
         ::interaction-component*))

(s/def ::interaction-components
  (s/and (s/coll-of ::interaction-component :kind vector? :into [])
         (fn [icomps]
           (when (seq icomps)
             (apply distinct? (map (some-fn
                                    :interaction-component/id
                                    #(get % "id")) icomps))))))


(s/def :definition/name
  ::language-map)

(s/def :definition/description
  ::language-map)

(s/def :definition/correctResponsesPattern
  (s/coll-of string? :kind vector?))

(s/def :definition/type
  ::iri)

(s/def :definition/moreInfo
  ::irl)

(s/def :definition/choices
  ::interaction-components)

(s/def :definition/scale
  ::interaction-components)

(s/def :definition/source
  ::interaction-components)

(s/def :definition/target
  ::interaction-components)

(s/def :definition/steps
  ::interaction-components)

(s/def :definition/extensions
  ::extensions)

(s/def :definition/interactionType
  #{"true-false"
    "choice"
    "fill-in"
    "long-fill-in"
    "matching"
    "performance"
    "sequencing"
    "likert"
    "numeric"
    "other"})

(def component-keys
  #{:definition/choices
    :definition/scale
    :definition/target
    :definition/steps
    :definition/source})

(def valid-component-keys
  "Given an interactionType, what component keys are valid?"
  {"choice"      #{:definition/choices}
   "sequencing"  #{:definition/choices}
   "likert"      #{:definition/scale}
   "matching"    #{:definition/source :definition/target}
   "performance" #{:definition/steps}
   "true-false"  #{}
   "fill-in"     #{}
   "numeric"     #{}
   "other"       #{}})

(defn valid-definition-component-keys?
  [data]
  "Predicate to ensure valid component list keys"
  (let [interaction-type (:definition/interactionType data)
        submitted-keys (intersection (set (keys data)) component-keys)
        valid-for-type (valid-component-keys interaction-type)
        invalid (difference submitted-keys valid-for-type)]

    (if (and interaction-type (seq invalid))
      false
      true)))

(s/def :activity/definition*
  (s/and
   (s/keys :opt [:definition/name
                 :definition/description
                 :definition/correctResponsesPattern
                 :definition/interactionType
                 :definition/type
                 :definition/moreInfo
                 :definition/choices
                 :definition/scale
                 :definition/source
                 :definition/target
                 :definition/steps
                 :definition/extensions])
   valid-definition-component-keys?))

(s/def :activity/definition
  (s/and
   (map-ns-conformer "definition")
   :activity/definition*))

(s/def :activity/objectType
  #{"Activity"})

(s/def :activity/id
  ::iri)

(s/def ::activity*
  (s/keys :req [:activity/id]
          :opt [:activity/objectType
                :activity/definition]))

(s/def ::activity
  (s/and
   (map-ns-conformer "activity")
   ::activity*))

;; Account

(s/def :account/name
  ::string-not-empty)

(s/def :account/homePage
  ::irl)

(s/def ::account*
  (s/keys :req [:account/name
                :account/homePage]))

(s/def ::account
  (s/and (map-ns-conformer "account")
         ::account*))
;; Agent

(s/def :agent/objectType
  #{"Agent"})

(s/def :agent/name
  string?)

(s/def :agent/mbox
  ::mailto-iri)

(s/def :agent/mbox_sha1sum
  ::sha1sum)

(s/def :agent/openid
  ::openid)

(s/def :agent/account
  ::account)

(defn max-one-ifi [a]
  (>= 1 (count (select-keys a ["mbox"
                               "mbox_sha1sum"
                               "openid"
                               "acount"
                               :agent/mbox
                               :agent/mbox_sha1sum
                               :agent/openid
                               :agent/account
                               :group/mbox
                               :group/mbox_sha1sum
                               :group/openid
                               :group/account]))))

(s/def ::agent*
  (s/keys :req [(or :agent/mbox
                    :agent/mbox_sha1sum
                    :agent/openid
                    :agent/account)]
          :opt [:agent/name
                :agent/objectType]))

(s/def ::agent
  (s/and
   (map-ns-conformer "agent")
   ::agent*))

;; Group

(s/def :group/objectType
  #{"Group"})

(s/def :group/name
  string?)

(s/def :group/mbox
  ::mailto-iri)

(s/def :group/mbox_sha1sum
  ::sha1sum)

(s/def :group/openid
  ::openid)

(s/def :group/account
  ::account)

(s/def :group/member
  (s/coll-of ::agent :kind vector? :into [] :gen-max 3))


(s/def ::identified-group
  (s/keys :req [:group/objectType
                (or :group/mbox
                    :group/mbox_sha1sum
                    :group/openid
                    :group/account)]
          :opt [:group/name
                :group/member]))

(s/def ::anonymous-group
  (s/and
   (s/keys :req [:group/objectType
                 :group/member]
           :opt [:group/name])
   #(-> % :group/member seq)))

(def identified-group?
  (comp
   some?
   (some-fn :group/mbox
            :group/mbox_sha1sum
            :group/openid
            :group/account)))

(defmulti group-type #(if (identified-group? %)
                        :group/identified
                        :group/anonymous))

(defmethod group-type :group/identified [_]
  ::identified-group)

(defmethod group-type :group/anonymous [_]
  ::anonymous-group)


(s/def ::group*
  (s/multi-spec group-type (fn [gen-val _]
                             gen-val)))

(s/def ::group
  (s/and
   (map-ns-conformer "group")
   ::group*))

;; Actor

(defmulti actor-type (fn [a]
                       (case (get a "objectType")
                         "Agent" :actor/agent
                         "Group" :actor/group
                         :actor/agent)))

(defmethod actor-type :actor/agent [_]
  ::agent)

(defmethod actor-type :actor/group [_]
  ::group)


(s/def ::actor
  (s/multi-spec actor-type (fn [gen-val _]
                             gen-val)))

;; Verb

(s/def :verb/id ::iri)

(s/def :verb/display ::language-map)

(s/def
  ::verb*
  (s/keys :req [:verb/id]
          :opt [:verb/display]))

(s/def ::verb
  (s/and
   (map-ns-conformer "verb")
   ::verb*))

;; Result

(s/def :score/scaled
  (s/double-in :min -1.0 :max 1.0))

(s/def :score/raw
  number?)

(s/def :score/min
  number?)

(s/def :score/max
  number?)

(s/def :result/score*
  (s/and (s/keys :opt [:score/scaled
                       :score/raw
                       :score/min
                       :score/max])
         (fn [{raw :score/raw
               min :score/min
               max :score/max}]
           (if (or min raw max)
             (apply <= (filter identity [min raw max]))
             true))))

(s/def :result/score
  (s/and
   (map-ns-conformer "score")
   :result/score*))

(s/def :result/success
  boolean?)

(s/def :result/completion
  boolean?)

(s/def :result/response
  string?)

(s/def :result/duration
  ::duration)

(s/def :result/extensions
  ::extensions)

(s/def ::result*
  (s/keys :opt [:result/score
                :result/success
                :result/completion
                :result/response
                :result/duration
                :result/extensions]))

(s/def ::result
  (s/and (map-ns-conformer "result")
         ::result*))

;; Statement Ref

(s/def :statement-ref/id ::uuid)

(s/def :statement-ref/objectType
  #{"StatementRef"})

(s/def ::statement-ref*
  (s/keys :req [:statement-ref/id
                :statement-ref/objectType]))

(s/def ::statement-ref
  (s/and (map-ns-conformer "statement-ref")
         ::statement-ref*))

;; Context

(s/def ::context-activities-array
  (s/coll-of ::activity :kind vector? :into [] :min-count 1))

(s/def ::context-activities
  (s/or ::context-activities-array
        ::context-activities-array
        ::activity
        ::activity))

(s/def :contextActivities/parent
  ::context-activities)

(s/def :contextActivities/grouping
  ::context-activities)

(s/def :contextActivities/category
  ::context-activities)

(s/def :contextActivities/other
  ::context-activities)

(s/def :context/contextActivities*
  (s/keys :opt [:contextActivities/parent
                :contextActivities/grouping
                :contextActivities/category
                :contextActivities/other]))

(s/def :context/contextActivities
  (s/and (map-ns-conformer "contextActivities")
         :context/contextActivities*))

(s/def :context/registration
  ::uuid)

(s/def :context/instructor
  ::actor)

(s/def :context/team
  ::group)

(s/def :context/revision
  string?)

(s/def :context/platform
  string?)

(s/def :context/language
  ::language-tag)

(s/def :context/statement
  ::statement-ref)

(s/def :context/extensions
  ::extensions)

(s/def ::context*
  (s/keys :opt [:context/registration
                :context/instructor
                :context/team
                :context/contextActivities
                :context/revision
                :context/platform
                :context/language
                :context/statement
                :context/extensions]))

(s/def ::context
  (s/and (map-ns-conformer "context")
         ::context*))

;; Attachments

(s/def :attachment/usageType
  ::iri)

(s/def :attachment/display
  ::language-map)

(s/def :attachment/description
  ::language-map)

(s/def :attachment/contentType
  string?)

(s/def :attachment/length
  int?)

(s/def :attachment/sha2
  ::sha2)

(s/def :attachment/fileUrl
  ::irl)

(s/def ::file-attachment
  (s/keys :req [:attachment/usageType
                :attachment/display
                :attachment/contentType
                :attachment/length
                :attachment/sha2]
          :opt [:attachment/description
                :attachment/fileUrl]))

(s/def ::url-attachment
  (s/keys :req [:attachment/usageType
                :attachment/display
                :attachment/contentType
                :attachment/length
                :attachment/sha2
                :attachment/fileUrl]
          :opt [:attachment/description]))

(s/def ::attachment*
  (s/keys :req [:attachment/usageType
                :attachment/display
                :attachment/contentType
                :attachment/length
                :attachment/sha2]
          :opt [:attachment/description
                :attachment/fileUrl]))

(s/def ::attachment
  (s/and (map-ns-conformer "attachment")
         ::attachment*))

(s/def ::attachments
  (s/coll-of ::attachment :kind vector? :into []))

;; Sub-statement

(s/def :sub-statement/actor
  ::actor)

(s/def :sub-statement/verb
  ::verb)

(defmulti sub-statement-object-type (fn [ss-o]
                                      (case (get ss-o "objectType")
                                        "Activity" :sub-statement-object/activity
                                        nil :sub-statement-object/activity
                                        "Agent" :sub-statement-object/agent
                                        "Group" :sub-statement-object/group
                                        "StatementRef" :sub-statement-object/statement-ref)))

(defmethod sub-statement-object-type :sub-statement-object/activity [_]
  ::activity)

(defmethod sub-statement-object-type :sub-statement-object/agent [_]
  ::agent)

(defmethod sub-statement-object-type :sub-statement-object/group [_]
  ::group)

(defmethod sub-statement-object-type :sub-statement-object/statement-ref [_]
  ::statement-ref)

(s/def :sub-statement/object
  (s/multi-spec sub-statement-object-type (fn [gen-val _]
                                            gen-val))
  #_(s/or
   ::activity
   ::activity
   ::actor
   ::actor
   ::statement-ref
   ::statement-ref))

(s/def :sub-statement/result
  ::result)

(s/def :sub-statement/context
  ::context)

(s/def :sub-statement/attachments
  ::attachments)

(s/def :sub-statement/timestamp
  ::timestamp)

(s/def :sub-statement/objectType
  #{"SubStatement"})

(s/def ::sub-statement*
  (s/and (s/keys :req [:sub-statement/actor
                       :sub-statement/verb
                       :sub-statement/object
                       :sub-statement/objectType]
                 :opt [:sub-statement/result
                       :sub-statement/context
                       :sub-statement/attachments
                       :sub-statement/timestamp])
         #(empty? (select-keys % [:sub-statement/id
                                  :sub-statement/stored
                                  :sub-statement/version
                                  :sub-statement/authority]))))

(s/def ::sub-statement
  (s/and (map-ns-conformer "sub-statement")
         ::sub-statement*))

;; Authority

(s/def ::oauth-consumer*
  (s/keys :req [:agent/account]))

(s/def ::oauth-consumer
  (s/and
   (map-ns-conformer "agent")
   ::oauth-consumer*))

(comment
  ;; three-legged oauth grp
  ;; FIXME: express these extra constraints
  (s/and :group/member
         (s/coll-of ::agent
                    :kind vector?
                    :count 2)
         (s/cat
          :oauth-consumer
          ::oauth-consumer
          :agent
          ::agent)))

(s/def ::three-legged-oauth-group*
  (s/and (s/keys :req [:group/objectType
                       :group/member]
                 :opt [:group/name
                       :group/mbox
                       :group/mbox_sha1sum
                       :group/openid
                       :group/account])
         #(some-> % :group/member count (= 2))
         ))

(s/def ::three-legged-oauth-group
  (s/and (map-ns-conformer "group")
         ::three-legged-oauth-group*
         ))


;; Statement!

(s/def :statement/authority
  ::actor)

(defmulti statement-object-type (fn [ss-o]
                                      (case (get ss-o "objectType")
                                        "Activity" :statement-object/activity
                                        nil :statement-object/activity
                                        "Agent" :statement-object/agent
                                        "Group" :statement-object/group
                                        "StatementRef" :statement-object/statement-ref
                                        "SubStatement" :statement-object/sub-statement)))

(defmethod statement-object-type :statement-object/activity [_]
  ::activity)

(defmethod statement-object-type :statement-object/agent [_]
  ::agent)

(defmethod statement-object-type :statement-object/group [_]
  ::group)

(defmethod statement-object-type :statement-object/statement-ref [_]
  ::statement-ref)

(defmethod statement-object-type :statement-object/sub-statement [_]
  ::sub-statement)


(s/def :statement/object
  (s/multi-spec statement-object-type (fn [gen-val _]
                                        gen-val)))

(s/def :statement/id
  ::uuid)

(s/def :statement/actor
  ::actor)

(s/def :statement/verb
  ::verb)

(s/def :statement/result
  ::result)

(s/def :statement/context
  ::context)

(s/def :statement/timestamp
  ::timestamp)

(s/def :statement/stored
  ::timestamp)

(s/def :statement/version
  ::version)

(s/def :statement/attachments
  ::attachments)

(s/def :statement/objectType
  #{"SubStatement"})

(def revision-or-platform?
  (some-fn :context/revision
           :context/platform
           ))

(s/def ::statement*
  (s/and
   (s/keys :req [:statement/actor
                 :statement/verb
                 :statement/object]
           :opt [:statement/id
                 :statement/result
                 :statement/context
                 :statement/timestamp
                 :statement/stored
                 :statement/authority
                 :statement/attachments])
   (fn valid-context? [s]
     (if (some-> s
                 :statement/object
                 :activity/objectType)
       true
       (not (some-> s :statement/context revision-or-platform?))))
   (fn valid-void? [s]
     (if (some-> s :statement/verb :verb/id (= "http://adlnet.gov/expapi/verbs/voided"))
       (some-> s :statement/object :statement-ref/objectType)
       true))))

(s/def ::statement
  (s/and
   (map-ns-conformer "statement")
   ::statement*))

(s/def ::statements
  (s/coll-of ::statement :kind vector? :into [] :min-count 1))

;; Shadow Core API
(def statement-checker
  (partial s/explain-data ::statement))

(def statements-checker
  (partial s/explain-data ::statements))

(def errors->data
  (comp ::spec-error ex-data))

(def errors->paths
  identity) ;; TODO: fix this if it is actually needed

(defn validate-statement [s]
  (if-let [error (statement-checker s)]
    (throw (ex-info "Invalid Statement"
                    {:type ::invalid-statement
                     ::spec-error error}))
    s))

(defn validate-statements [ss]
  (if-let [error (statements-checker ss)]
    (throw (ex-info "Invalid Statements"
                    {:type ::invalid-statements
                     ::spec-error error}))
    ss))