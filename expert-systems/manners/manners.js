goog.addDependency("base.js", ['goog'], []);
goog.addDependency("../cljs/core.js", ['cljs.core'], ['goog.string', 'goog.array', 'goog.object', 'goog.string.StringBuffer']);
goog.addDependency("../manners/facts.js", ['manners.facts'], ['cljs.core']);
goog.addDependency("../manners/rules.js", ['manners.rules'], ['cljs.core']);
goog.addDependency("../manners/templates.js", ['manners.templates'], ['cljs.core']);
goog.addDependency("../manners/functions.js", ['manners.functions'], ['cljs.core']);
goog.addDependency("../manners/core.js", ['manners.core'], ['cljs.core', 'manners.rules', 'manners.facts', 'manners.templates', 'manners.functions']);