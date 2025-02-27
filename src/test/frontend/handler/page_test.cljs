(ns frontend.handler.page-test
  (:require [cljs.test :refer [deftest are]]
            [clojure.string :as string]
            [frontend.util :as util]
            [frontend.handler.page :as page-handler]))

(defn- replace-page-ref!
  [content old-name new-name]
  (let [[original-old-name original-new-name] (map string/trim [old-name new-name])
        [old-ref new-ref] (map #(util/format "[[%s]]" %) [old-name new-name])
        [old-name new-name] (map #(when (string/includes? % "/")
                                    (string/replace % "/" "."))
                                 [original-old-name original-new-name])
        old-org-ref (re-find
                     (re-pattern
                      (util/format
                       "\\[\\[file:\\./.*%s\\.org\\]\\[(.*?)\\]\\]" old-name))
                     content)]
    (-> (if old-org-ref
          (let [[old-full-ref old-label] old-org-ref
                new-label (if (= old-label original-old-name)
                            original-new-name
                            old-label)
                new-full-ref (-> (string/replace old-full-ref old-name new-name)
                                 (string/replace (str "[" old-label "]")
                                                 (str "[" new-label "]")))]
            (string/replace content old-full-ref new-full-ref))
          content)
        (string/replace old-ref new-ref))))

(defn- replace-old-page!
  [content old-name new-name]
  (when (and (string? content) (string? old-name) (string? new-name))
    (-> content
        (replace-page-ref! old-name new-name)
        (page-handler/replace-tag-ref! old-name new-name))))

(deftest test-replace-page-ref!
  (are [x y] (= (let [[content old-name new-name] x]
                  (replace-page-ref! content old-name new-name))
                y)
    ["bla [[foo]] bla" "foo" "bar"] "bla [[bar]] bla"

    ["bla [[logseq/foo]] bla" "logseq/foo" "logseq/bar"] "bla [[logseq/bar]] bla"

    ["bla [[file:./logseq.foo.org][logseq/foo]] bla" "logseq/foo" "logseq/bar"]
    "bla [[file:./logseq.bar.org][logseq/bar]] bla"

    ["bla [[file:./pages/logseq.foo.org][logseq/foo]] bla" "logseq/foo" "logseq/bar"]
    "bla [[file:./pages/logseq.bar.org][logseq/bar]] bla"

    ["bla [[file:./pages/logseq.foo.org][logseq/foo]] bla [[logseq/foo]]" "logseq/foo" "logseq/bar"]
    "bla [[file:./pages/logseq.bar.org][logseq/bar]] bla [[logseq/bar]]"

    ["bla [[file:./pages/logseq.foo.org][don't change this label]] bla [[logseq/foo]]" "logseq/foo" "logseq/bar"]
    "bla [[file:./pages/logseq.bar.org][don't change this label]] bla [[logseq/bar]]"))

(deftest test-replace-tag-ref!
  (are [x y] (= (let [[content old-name new-name] x]
                  (page-handler/replace-tag-ref! content old-name new-name))
                y)
       ["#foo" "foo" "bar"] "#bar"
       ["#foo" "foo" "new bar"] "#[[new bar]]"

       ["bla #foo bla" "foo" "bar"] "bla #bar bla"
       ["bla #foo bla" "foo" "new bar"] "bla #[[new bar]] bla"

       ["bla #foo" "foo" "bar"] "bla #bar"
       ["bla #foo" "foo" "new bar"] "bla #[[new bar]]"

       ["#foo #foobar bar#foo #foo" "foo" "bar"]
       "#bar #foobar bar#foo #bar"
       
       ["#foo #foobar bar#foo #foo" "foo" "new bar"]
       "#[[new bar]] #foobar bar#foo #[[new bar]]"

       ["#logseq/foo #logseq/foobar bar#logseq/foo #logseq/foo" "logseq/foo" "logseq/bar"]
       "#logseq/bar #logseq/foobar bar#logseq/foo #logseq/bar"))

(deftest test-replace-old-page!
  (are [x y] (= (let [[content old-name new-name] x]
                  (replace-old-page! content old-name new-name))
                y)
       ["#foo bla [[foo]] bla #foo" "foo" "bar"]
       "#bar bla [[bar]] bla #bar"

       ["#logseq/foo bla [[logseq/foo]] bla [[file:./pages/logseq.foo.org][logseq/foo]] bla #logseq/foo" "logseq/foo" "logseq/bar"]
       "#logseq/bar bla [[logseq/bar]] bla [[file:./pages/logseq.bar.org][logseq/bar]] bla #logseq/bar"))

#_(cljs.test/test-ns 'frontend.handler.page-test)
