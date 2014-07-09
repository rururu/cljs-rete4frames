goog.addDependency("base.js", ['goog'], []);
goog.addDependency("../cljs/core.js", ['cljs.core'], ['goog.string', 'goog.array', 'goog.object', 'goog.string.StringBuffer']);
goog.addDependency("../mab/facts.js", ['mab.facts'], ['cljs.core']);
goog.addDependency("../mab/rules.js", ['mab.rules'], ['cljs.core']);
goog.addDependency("../mab/templates.js", ['mab.templates'], ['cljs.core']);
goog.addDependency("../mab/functions.js", ['mab.functions'], ['cljs.core']);
goog.addDependency("../mab/core.js", ['mab.core'], ['cljs.core', 'mab.templates', 'mab.functions', 'mab.facts', 'mab.rules']);