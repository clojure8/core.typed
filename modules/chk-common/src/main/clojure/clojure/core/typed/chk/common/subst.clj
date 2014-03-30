(ns clojure.core.typed.chk.common.subst
  (:require [clojure.core.typed.chk.common.type-rep :as r]
            [clojure.core.typed.chk.common.utils :as u]
            [clojure.core.typed.chk.common.fold-rep :as f]
            [clojure.core.typed.chk.common.type-ctors :as tc]
            [clojure.core.typed.chk.common.frees :as frees]
            [clojure.core.typed.chk.common.cs-rep :as crep]
            [clojure.core.typed.chk.common.filter-rep :as fl]
            [clojure.core.typed.chk.common.filter-ops :as fo]
            [clojure.core.typed.chk.common.object-rep :as orep]
            [clojure.core.typed :as t :refer [ann Seqable]])
  (:import (clojure.core.typed.chk.common.type_rep F Function HeterogeneousVector)
           (clojure.lang Symbol)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Variable substitution


(t/tc-ignore
(derive ::substitute f/fold-rhs-default)
(f/add-fold-case ::substitute
               F
               (fn [{name* :name :as f} {{:keys [name image]} :locals}]
                 (if (= name* name)
                   image
                   f)))
  )

(ann ^:no-check substitute [r/Type Symbol r/Type -> r/Type])
(defn substitute [image name target]
  {:pre [(r/AnyType? image)
         (symbol? name)
         (r/AnyType? target)]
   :post [(r/AnyType? %)]}
  (f/fold-rhs ::substitute
              {:locals {:name name
                        :image image}}
              target))

(ann ^:no-check substitute-many [r/Type (U nil (Seqable r/Type)) (U nil (Seqable Symbol))
                                 -> r/Type])
(defn substitute-many [target images names]
  (reduce (fn [t [im nme]] (substitute im nme t))
          target
          (map vector images names)))

(declare substitute-dots substitute-dotted)

(ann ^:no-check subst-all [crep/SubstMap r/Type -> r/Type])
(defn subst-all [s t]
  {:pre [(crep/substitution-c? s)
         (r/AnyType? t)]
   :post [(r/AnyType? %)]}
  (u/p :subst/subst-all
  (reduce (fn [t [v r]]
            (cond
              (crep/t-subst? r) (substitute (:type r) v t)
              (crep/i-subst? r) (substitute-dots (:types r) nil v t)
              (crep/i-subst-starred? r) (substitute-dots (:types r) (:starred r) v t)
              (and (crep/i-subst-dotted? r)
                   (empty? (:types r))) (substitute-dotted (:dty r) (:name (:dbound r)) v t)
              (crep/i-subst-dotted? r) (throw (Exception. "i-subst-dotted nyi"))
              :else (throw (Exception. "Other substitutions NYI"))))
          t s)))

;; Substitute dots


(t/tc-ignore
(derive ::substitute-dots f/fold-rhs-default)
(f/add-fold-case ::substitute-dots
  Function
  (fn [{:keys [dom rng rest drest kws] :as ftype} {{:keys [name sb images rimage]} :locals}]
   (assert (not kws) "TODO substitute keyword args")
   (if (and drest
            (= name (:name drest)))
     (r/Function-maker (doall
                         (concat (map sb dom)
                                 ;; We need to recur first, just to expand out any dotted usages of this.
                                 (let [expanded (sb (:pre-type drest))]
                                   ;(prn "expanded" (unparse-type expanded))
                                   (map (fn [img] (substitute img name expanded)) images))))
                       (sb rng)
                       rimage nil nil)
     (r/Function-maker (doall (map sb dom))
                       (sb rng)
                       (and rest (sb rest))
                       (and drest (r/DottedPretype1-maker (sb (:pre-type drest))
                                                          (:name drest)))
                       nil))))

(f/add-fold-case ::substitute-dots
  HeterogeneousVector
  (fn [{:keys [types fs objects rest drest] :as ftype} {{:keys [name sb images rimage]} :locals}]
   (if (and drest
            (= name (:name drest)))
     (r/-hvec (doall
                (concat (map sb types)
                        ;; We need to recur first, just to expand out any dotted usages of this.
                        (let [expanded (sb (:pre-type drest))]
                          ;(prn "expanded" (unparse-type expanded))
                          (map (fn [img] (substitute img name expanded)) images))))
              :filters (doall (concat (map sb fs) (repeat (count images) (fo/-FS fl/-top fl/-top))))
              :objects (doall (concat (map sb objects) (repeat (count images) orep/-empty))))
     (r/-hvec (doall (map sb types))
              :filters (doall (map sb fs))
              :objects (doall (map sb objects))
              :rest (when rest (sb rest))
              :drest (when drest (r/DottedPretype1-maker (sb (:pre-type drest))
                                                         (:name drest)))))))
  )

;; implements angle bracket substitution from the formalism
;; substitute-dots : Listof[Type] Option[type] Name Type -> Type
(ann ^:no-check substitute-dots [(U nil (Seqable r/Type)) (U nil r/Type) Symbol r/Type -> r/Type])
(defn substitute-dots [images rimage name target]
  {:pre [(every? r/AnyType? images)
         ((some-fn nil? r/AnyType?) rimage)
         (symbol? name)
         (r/AnyType? target)]}
  ;(prn "substitute-dots" (unparse-type target) name "->" (map unparse-type images))
  (letfn [(sb [t] (substitute-dots images rimage name t))]
    (if (or ((frees/fi target) name)
            ((frees/fv target) name))
      (f/fold-rhs ::substitute-dots 
                {:type-rec sb
                 :filter-rec (f/sub-f sb ::substitute-dots)
                 :locals {:name name
                          :sb sb
                          :images images
                          :rimage rimage}}
                target)
      target)))


(t/tc-ignore
(derive ::substitute-dotted f/fold-rhs-default)
(f/add-fold-case ::substitute-dotted
  F
  (fn [{name* :name :as t} {{:keys [name image]} :locals}]
   (if (= name* name)
     image
     t)))

(f/add-fold-case ::substitute-dotted
  Function
  (fn [{:keys [dom rng rest drest kws]} {{:keys [sb name image]} :locals}]
   (assert (not kws))
   (r/Function-maker (doall (map sb dom))
                     (sb rng)
                     (and rest (sb rest))
                     (and drest
                          (r/DottedPretype1-maker (substitute image (:name drest) (sb (:pretype drest)))
                                                  (if (= name (:name drest))
                                                    name
                                                    (:name drest))))
                     nil)))

(f/add-fold-case ::substitute-dotted
  HeterogeneousVector
  (fn [{:keys [types fs objects rest drest]} {{:keys [sb name image]} :locals}]
    (r/-hvec (doall (map sb types))
             :filters (doall (map sb fs))
             :objects (doall (map sb objects))
             :rest (when rest (sb rest))
             :drest (when drest
                      (r/DottedPretype1-maker (substitute image (:name drest) (sb (:pretype drest)))
                                              (if (= name (:name drest))
                                                name
                                                (:name drest)))))))
  )

;; implements curly brace substitution from the formalism
;; substitute-dotted : Type Name Name Type -> Type
(ann ^:no-check substitute-dotted [r/Type Symbol Symbol r/Type -> r/Type])
(defn substitute-dotted [image image-bound name target]
  {:pre [(r/AnyType? image)
         (symbol? image-bound)
         (symbol? name)
         (r/AnyType? target)]
   :post [(r/AnyType? %)]}
  (letfn [(sb [t] (substitute-dotted image image-bound name t))]
    (if ((frees/fi target) name)
      (f/fold-rhs ::substitute-dotted
                {:type-rec sb 
                 :filter-rec (f/sub-f sb ::substitute-dotted)
                 :locals {:name name
                          :sb sb
                          :image image}}
                target
                target))))