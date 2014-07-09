goog.addDependency("base.js", ['goog'], []);
goog.addDependency("../cljs/core.js", ['cljs.core'], ['goog.string', 'goog.array', 'goog.object', 'goog.string.StringBuffer']);
goog.addDependency("../zebra/facts.js", ['zebra.facts'], ['cljs.core']);
goog.addDependency("../zebra/rules.js", ['zebra.rules'], ['cljs.core']);
goog.addDependency("../zebra/functions.js", ['zebra.functions'], ['cljs.core']);
goog.addDependency("../zebra/templates.js", ['zebra.templates'], ['cljs.core']);
goog.addDependency("../zebra/core.js", ['zebra.core'], ['cljs.core', 'zebra.rules', 'zebra.functions', 'zebra.templates', 'zebra.facts']);