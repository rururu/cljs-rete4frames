(ns zebra.functions
  (:require-macros [zebra.macro :as macro]))

(macro/create-eval-for ['not= not=
                        'not-exists zebra.core/not-exists
                        'asser zebra.core/asser
                        'modify zebra.core/modify
                        'retract zebra.core/retract
                        'println println
                        'str str
                        '= =
                        '+ +
                        '- -
                        'not not
                        'nil? nil?])
