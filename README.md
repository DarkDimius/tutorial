### scala.meta tutorial: Exploring semantics

[![Join the chat at https://gitter.im/scalameta/scalameta](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scalameta/scalameta?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

### Quick links

You can follow the guide by going through [commits in the branch](https://github.com/scalameta/tutorial/commits/exploring-semantics) or by jumping straight into the working solution at [the head of the branch](https://github.com/scalameta/tutorial/tree/exploring-semantics).

### Problem statement

In the [view-bounds guide](https://github.com/scalameta/tutorial/tree/6dc51fc68e9ad235a0ea2215bbc725695b65a9e9), we've discovered syntactic APIs of scala.meta. Without requiring any external dependencies, scala.meta is capable of robustly parsing, transforming and prettyprinting Scala code.

However, platform-independent functionality in scala.meta has its limits. Since developing a typechecker for Scala is a virtually insurmountable task that requires significant time investments even by [the](http://lamp.epfl.ch/) [select](http://www.jetbrains.com/) [few](http://www.typesafe.com/) who can undertake it, we decided to cut corners.

In order to perform semantic operations (functionality that goes beyond obtaining and traversing program structure as written in source files), we require a semantic context, i.e. an implicit value that conforms to [a special interface](https://github.com/scalameta/scalameta/blob/master/scalameta/semantic/src/main/scala/scala/meta/semantic/Context.scala).

Semantic contexts are provided by platform-dependent libraries, called hosts, and in this tutorial we will explore semantics of scala.meta trees with scalahost, a scalac-based implementation of the aforementioned Context interface. After completing the tutorial, it will become clear how to obtain semantic information about scala.meta trees at compile-time and at runtime.

### Anytime metaprogramming

In many metaprogramming frameworks, full semantic information about the program (signatures of local definitions, method bodies, etc) only exists at compile time, so in order to explore this realm it is necessary to write compiler plugins or use other means of compile-time reflection, e.g. macros.

One of the key innovations of scala.meta is introduction of AST persistence that mandates saving typechecked abstract syntax trees into binaries produced by the compiler. Our working group at LAMP has produced a specification of [the TASTY format](https://docs.google.com/document/d/1Wp86JKpRxyWTqUU39H40ZdXOlacTNs20aTj7anZLQDw/edit#heading=h.foemem8hq66y), a binary representation for typed Scala ASTs. Our scalac plugin saves scala.meta trees in TASTY format and provides ways to load scala.meta trees back from binaries produced by TASTY-compliant Scala compilers.

With the introduction of TASTY, the restrictions on program introspection are completely lifted, and the distinction between compile-time and runtime metaprogramming becomes obsolete. In this tutorial, we will illustrate this thesis in practice.

### Configuring the build system

We will be using a two-project configuration that consists of `scrutinee`, a project under inspection, and `explorer`, a scala.meta-based tool that loads semantic information from TASTY.

To enable AST persistence, it is necessary to reference the `"org.scalameta" %% "scalahost" % "..."` compiler plugin. To read TASTY into scala.meta trees, it is necessary to reference that compiler plugin as a library in order to gain access to a TASTY-based semantic context.

### To be continued

I need to fix some compiler crashes in order to finish the guide. Will do that a bit later, stay tuned!