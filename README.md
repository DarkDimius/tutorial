### scala.meta tutorial: Exploring semantics

[![Join the chat at https://gitter.im/scalameta/scalameta](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scalameta/scalameta?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

### Quick links

You can follow the guide by going through [commits in the branch](https://github.com/scalameta/tutorial/commits/exploring-semantics) or by jumping straight into the working solution at [the head of the branch](https://github.com/scalameta/tutorial/tree/exploring-semantics).

### Problem statement

In the [view-bounds guide](https://github.com/scalameta/tutorial/tree/6dc51fc68e9ad235a0ea2215bbc725695b65a9e9), we've discovered syntactic APIs of scala.meta. Without requiring any external dependencies, scala.meta is capable of robustly parsing, transforming and prettyprinting Scala code.

However, platform-independent functionality in scala.meta has its limits. Since developing a typechecker for Scala is a virtually insurmountable task that requires significant time investments even by [the](http://lamp.epfl.ch/) [select](http://www.jetbrains.com/) [few](http://www.typesafe.com/) who can undertake it, we decided to cut corners.

In order to perform semantic operations (functionality that goes beyond obtaining and traversing program structure as written in source files), we require a semantic context, i.e. an implicit value that conforms to [a special interface](https://github.com/scalameta/scalameta/blob/master/scalameta/semantic/src/main/scala/scala/meta/semantic/Context.scala).

Semantic contexts are provided by platform-dependent libraries, called hosts, and in this tutorial we will explore semantics of scala.meta trees with scalahost, a scalac-based implementation of the aforementioned Context interface. After completing the tutorial, it will become clear how to obtain semantic information about scala.meta trees at compile-time and at runtime.