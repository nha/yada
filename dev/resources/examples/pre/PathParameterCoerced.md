In this example we want to show how to path parameters can be given a type and how they are automatically coerced to the specified type.

We want __account__ to be extracted as a `java.lang.Long`, and __account-type__ to be extracted as a Clojure keyword.

<resource-map/>

We form the request to contain 2 path parameters, so the path is of the form .../_account-type_/_account_.

<request/>

<response/>
