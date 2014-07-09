(ns mab.functions
  (:require-macros [mab.macro :as macro]))

(macro/create-eval-for ['not= not=
                        'not-exists mab.core/not-exists
                        'asser mab.core/asser
                        'modify mab.core/modify
                        'retract mab.core/retract
                        'println println
                        'str str])
