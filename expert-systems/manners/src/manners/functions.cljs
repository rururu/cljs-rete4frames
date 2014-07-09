(ns manners.functions
  (:require-macros [manners.macro :as macro]))

(macro/create-eval-for ['not= not=
                        'not-exists manners.core/not-exists
                        'asser manners.core/asser
                        'modify manners.core/modify
                        'retract manners.core/retract
                        'println println
                        'str str
                        'inc inc])
