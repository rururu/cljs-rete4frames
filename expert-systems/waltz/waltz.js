goog.addDependency("base.js", ['goog'], []);
goog.addDependency("../cljs/core.js", ['cljs.core'], ['goog.string', 'goog.array', 'goog.object', 'goog.string.StringBuffer']);
goog.addDependency("../waltz/facts.js", ['waltz.facts'], ['cljs.core']);
goog.addDependency("../waltz/rules.js", ['waltz.rules'], ['cljs.core']);
goog.addDependency("../waltz/functions.js", ['waltz.functions'], ['cljs.core']);
goog.addDependency("../waltz/templates.js", ['waltz.templates'], ['cljs.core']);
goog.addDependency("../waltz/core.js", ['waltz.core'], ['waltz.rules', 'cljs.core', 'waltz.functions', 'waltz.templates', 'waltz.facts']);