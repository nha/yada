# The yada user guide

Welcome to the yada user guide!

This guide is ideal if you are new to yada and is suitable for anyone
who is interested in building web APIs with Clojure. Previous experience
of web development in Clojure may be helpful but is not mandatory.

If you follow this guide carefully you will learn how to take advantage of the many features yada has to offer.

## Introduction

A web API is a set of web resources that are accessed by clients, called
_user agents_, using the HTTP protocol. Typically, a developer will
build a web API to expose the functionality of a software system to
users and other agents across the web.

HTTP is a large and powerful protocol — yada is a library that helps reduce the amount of coding required for meeting its requirements.

### Where yada fits

There is a great number of libraries available to Clojure programmers to
help develop web applications. Let's explain where yada fits into a Clojure web application.

During a web request, a browser (or other user agent) establishes a connection with a web
server and sends a request encoded according to the HTTP standard. The
server decodes the request and builds a ordinary Clojure map, called a
_request map_, which it passes as an argument to a Clojure function, called a
_handler_. The [Ring](https://github.com/ring) project establishes
a standard set of keywords so that numerous compatible web servers can
fulfill this rôle, either natively (e.g. aleph, http-kit) or via a
bridge (e.g. Jetty, or any Java servlet container).

#### First we route to the target handler...

Typically, the handler that the web server calls looks at the URI
contained in the request and delegates the request to another
handler. We say that the handler _dispatches_ the request, based on the
URI. While this routing function can be written by the Clojure
developer, there exist a number of libraries dedicated to this task
which can be used with yada.

The target handler is responsible for returning a ordinary Clojure map,
called a _response map_. The Ring standard states that such responses should contain a __:status__ entry indicating the HTTP response code, an optional __:headers__ entry containing the response headers, and an optional __:body__ entry containing the response's entity body.

#### Then we code the response...

Usually, the target handler is developed by the application developer. But with yada, the application developer passes a ordinary Clojure map, called a _resource map_ to a special yada function, `yada.core/handler`, which returns the target handler.

If you are unfamiliar with web development in Clojure, let's explain
that again using some basic Clojure code. Here is a simple Ring-compatible handler function :-

```clojure
(defn hello "My simple handler" [req]
  {:status 200, :body "Hello World!"})
```

This declares a function (in the var `hello`) that accepts a single argument (`req`) known as the _request map_. The implementation of the function returns a literal map, using Clojure's `{}` syntax. (By the way, commas are optional in Clojure).

Compare this with how to create an equivalent function using yada.

```clojure
(require '[yada.core :refer (handler)])
(def hello (handler {:body "Hello World!"}))
```

This calls a function built-in to yada called `handler`, with a single argument called a _resource map_. In this example, the resource map looks strikingly similar to a Ring response map but don't be deceived, there is a lot more going on under the hood as we shall soon see.

<include type="note" ref="modular-template-intro"/>

### Resource maps

Let's look in more detail at the _resource map_ you pass to yada's `handler` function to create a Ring handler. This map is an ordinary Clojure map. Let's demonstrate using the example we have already seen.

<example ref="HelloWorld"/>

We'll use more examples like that to show different resource maps that demonstrate all the features of yada. (If you want to experiment further, why not take a minute to [create](#modular-template-intro) your own test project?)

<include type="note" ref="liberator"/>

Resources maps are the key to understanding yada. They are used to
define all the ways that a handler should respond to a web request.

### Dynamic responses

In the previous example, the __:body__ entry was declared as a string value. But often the body will be created dynamically, depending on the current state of the resource and/or parameters passed in the request. We need some way to vary the body at runtime. We simply replace the string value in the resource map with a function.

<example ref="DynamicHelloWorld"/>

The function in the example above takes a single argument known as the _request context_. It is an ordinary Clojure map containing various data relating to the request. Request contexts will be covered in more detail later on.

### Asynchronous responses

Under normal circumstances, with Clojure running on a JVM, each request can be processed by a separate thread.

However, sometimes the production of the response body involves making requests
to data-sources and other activities which may be _I/O-bound_. This means the thread handling the request has to block, waiting on the data to arrive from the IO system.

For heavily loaded or high-throughput web APIs, this is an inefficient
use of precious resources. In recent years, this problem has been
addressed by using a asynchronous I/O. The request thread
is able to make a request for data via I/O, and then is free to carry out
further work (such as processing another web request). When the data
requested arrives on the I/O channel, another thread carries on when the
original thread left off.

As a developer, yada gives you fine-grained control over when to use a synchronous
programming model and when to use an asynchronous one.

#### Deferred values

A deferred value is a simply value that may not yet be known. Examples
include Clojure's futures, delays and promises. Deferred values are
built into yada. For further details, see Zach Tellman's
[manifold](https://github.com/ztellman/manifold) library.

In almost all cases, it is possible to specify _deferred values_ in a
resource map.

Let's see this in action :-

<example ref="AsyncHelloWorld"/>

The sleep is exaggerated but the delay that the web client would
experience is real. In a real-world application, however, the ability to
use an asynchronous model is very useful for techniques to improve
scalability. For example, in a heavily loaded server, I/O operations can
be queued and batched together. Performance may be slightly worse for
each individual request, but the overall throughput of the web server
can be significantly improved.

Normally, exploiting asynchronous operations in handling web requests is
difficult and requires advanced knowledge of asynchronous programming
techniques. In yada, however, it is very easy.

<include type="note" ref="ratpack"/>

<example ref="StatusAndHeaders"/>

## Content Negotiation

<example ref="BodyContentTypeNegotiation"/>
<example ref="BodyContentTypeNegotiation2"/>

## Resources

<example ref="ResourceExists"/>
<example ref="ResourceFunction"/>
<example ref="ResourceExistsAsync"/>
<example ref="ResourceDoesNotExist"/>
<example ref="ResourceDoesNotExistAsync"/>

## Parameters

Parameters are an important part of many requests and since APIs form
the basis of integration between software, it is useful to be able to
declare parameter expectations.

These expectations form the basis of a contract between a user-agent and
server. If a user-agent does not meet the contract set by the API, the
server can respond with a 400 status code, indicating that the request
is malformed.

<example ref="PathParameter"/>
<example ref="PathParameterDeclared"/>

## State

REST is about resources which have state, and representations that are
negotiated between the user agent and the server to transfer that state.

<example ref="ResourceState"/>
<example ref="ResourceStateWithBody"/>
<example ref="ResourceStateTopLevel"/>

## Conditional Requests

<example ref="LastModifiedHeader"/>
<example ref="LastModifiedHeaderAsLong"/>
<example ref="LastModifiedHeaderAsDeferred"/>
<example ref="IfModifiedSince"/>


## Puts

<example ref="PutResourceMatchedEtag"/>
<example ref="PutResourceUnmatchedEtag"/>

## Service Availability

<example ref="ServiceUnavailable"/>
<example ref="ServiceUnavailableAsync"/>
<example ref="ServiceUnavailableRetryAfter"/>
<example ref="ServiceUnavailableRetryAfter2"/>
<example ref="ServiceUnavailableRetryAfter3"/>


## Validation

<example ref="DisallowedPost"/>
<example ref="DisallowedGet"/>
<example ref="DisallowedPut"/>
<example ref="DisallowedDelete"/>
