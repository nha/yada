### Trying things out in your own project

Simple examples help to explain a concept but don't show how to create a
complete application.

If you would like to see how an application exposing a yada-based web
API is assembled, you can generate one with JUXT's
[modular](https://github.com/juxt/modular) template system. A __yada__ template exists that you can use to generate a Clojure project structure from a console with the following command :-

```shell
lein new modular hacking-with-yada yada
```

This creates an application directory named __hacking-with-yada__ that
demonstrates many of yada's features. You can replace __hacking-with-yada__
with a name of your own invention. To run and develop the application,
follow the instructions in the `README.md` file that is generated inside
the project directory.

(This requires that you have a fairly recent version (2.3.0+) of Leiningen installed. If you don't have the `lein` command on your system, visit the [Leiningen website](http://leiningen.org) for installation details)
