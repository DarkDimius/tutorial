### scala.meta tutorial: Automatic migration for view bounds

[![Join the chat at https://gitter.im/scalameta/scalameta](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scalameta/scalameta?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

### Quick links

You can follow the guide by going through [commits in the branch](https://github.com/scalameta/tutorial/commits/view-bounds) or by jumping straight into the working solution at [the head of the branch](https://github.com/scalameta/tutorial/tree/view-bounds).

### Problem statement

As discussed in [SI-7629](https://issues.scala-lang.org/browse/SI-7629) and implemented in [#2909](https://github.com/scala/scala/pull/2909), view bounds are almost ready to be deprecated. There is a consensus that this language feature is unnecessary, there is an understanding of how to replace usages of view bounds, the compiler already supports deprecation warnings for view bounds (under `-Xfuture`).

The only thing that's missing is a migration tool that would automatically rewrite view bounds into equivalent code. In this guide, we are going to write such a tool using functionality provided by scala.meta.

Over the course of the guide we will be developing and testing our migration tool on an excerpt from the standard library that lives in
[scala/math/Ordering.scala](https://github.com/scala/scala/blob/v2.11.7/src/library/scala/math/Ordering.scala). It is non-trivial enough to make things interesting:

```scala
package scala
package math

import java.util.Comparator
import scala.language.{implicitConversions, higherKinds}

// Skipping some code from scala/math/Ordering.scala

trait LowPriorityOrderingImplicits {
  /** This would conflict with all the nice implicit Orderings
   *  available, but thanks to the magic of prioritized implicits
   *  via subclassing we can make `Ordered[A] => Ordering[A]` only
   *  turn up if nothing else works.  Since `Ordered[A]` extends
   *  `Comparable[A]` anyway, we can throw in some Java interop too.
   */
  implicit def ordered[A <% Comparable[A]]: Ordering[A] = new Ordering[A] {
    def compare(x: A, y: A): Int = x compareTo y
  }
}

// Skipping some more code from scala/math/Ordering.scala
```

[![Join the chat at https://gitter.im/scalameta/scalameta](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scalameta/scalameta?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)