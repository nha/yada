The path in the URI is `/PathParameter/1234`. Let's assume that our routing library has extracted the account number (`1234`) and provided it in the Ring request's `:route-params` entry.

```clojure
{
  :account "1234"
  ...
}`
```

The code in __:body__ can reach into the _request context_ for the Ring request, and from that extract the `:route-params` entry for `:account`.
