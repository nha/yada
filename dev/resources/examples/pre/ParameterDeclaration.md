If a parameter is declared, its default parameter types is a string. However, in our example, account numbers are longs, so we declare the type accordingly.

```
{:account {:type Long}}
```

There are a number of good reasons to declare types.

- the parameter will undergo a validity check, to ensure the client is sending something that can be turned into the declared type.
- the parameter declaration can be used in the publication of API documentation (see Swagger).
- the parameter value will be automatically coerced to the given type. This means less code to write.

When types are declared, they are extracted from the request and made available via the __:params__ entry in the _request context_.

The value of __:type__ is interpreted by Prismatic's Schema library. Below is a table listing examples.

<table>
<thead>
<tr>
<th>:type</th>
<th>Java type</th>
</tr>
</thead>
<tbody>
<tr><td>schema.core/Str</td><td>java.lang.String (the default)</td></tr>
<tr><td>schema.core/Int</td><td>java.lang.Integer</td></tr>
<tr><td>schema.coreInst</td><td>java.util.Date</td></tr>
</tbody>
</table>

Remember, Clojure automatically boxes and unboxes Java primitives, so you can treat a java.lang.Integer value as a Java primitive.
