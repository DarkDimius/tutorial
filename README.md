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

### Configuring the build system

In order to configure your build system to pick up scala.meta, you need just a single `libraryDependencies` line. The exact shape of the entry depends on the tasks that you're going to perform:
  * Syntactic operations: `libraryDependencies += "org.scalameta" %% "scalameta" % "..."` (platform-independent data structures/APIs + platform-independent tokenizer and parser)
  * Semantic operations: `libraryDependencies += "org.scalameta" %% "scalahost" % "..."` (the same as above + platform-dependent typechecker implemented on top of scalac)

In our case, we need to do a purely syntactic rewriting, so the first line is going to suffice. Refer to other guides to see how to proceed with semantic APIs.

Despite being one-liners, both scalameta and scalahost actually refer to a bunch of submodules, which means that if necessary you can be really fine-grained about the functionality that you want to include. In our case, it'd be enough to reference `libraryDependencies += "org.scalameta" %% "quasiquotes" % "..."`, but we'll proceed with `"scalameta"` for simplicity.

![Scala.meta modules](https://rawgit.com/scalameta/scalameta/master/docs/modules.svg)

### Setting up imports

Since scala.meta APIs aren't living in a cake (looking at you, `scala.tools.nsc` and `scala.reflect`), setting up the environment is fairly simple and typically takes just two lines of code.

1) As we've referenced `"scalameta"`, it's enough to just `import scala.meta._` to bring all the functionality into current scope. When referencing individual modules, the umbrella import is not available, and one has to import individual pieces of functionality separately (e.g. `import scala.meta.parsers._` or `import scala.meta.quasiquotes._`).

2) In order to activate syntactic APIs, it's also necessary to select a dialect that will guide tokenization and parsing. Selection is performed by bringing a corresponding implicit value in scope. The list of available dialects is provided in [dialects/package.scala](/scalameta/dialects/src/main/scala/scala/meta/dialects/package.scala#L43). For now, we pick `import scala.meta.dialects.Scala211`.

```scala
import scala.meta._
import scala.meta.dialects.Scala211

object Test {
  def main(args: Array[String]): Unit = {
    ???
  }
}

### Parsing the excerpt

After the initial configuration is done, utilizing scala.meta APIs is fairly easy. For instance, parsing something is a one-liner, as long as the type of the something is understood by scala.meta:

```scala
import scala.meta._
import scala.meta.dialects.Scala211

object Test {
  def main(args: Array[String]): Unit = {
    val stream = getClass.getResourceAsStream("Ordering.scala")
    val tree = stream.parse[Source]
  }
}
```

`parse` showcases the power of implicits in enabling lightweight, configurable and modular APIs. Here's the code that defines this functionality in scala.meta:

```scala
private[meta] trait Api {
  implicit class XtensionParseInputLike[T](inputLike: T) {
    def parse[U](implicit convert: Convert[T, Input], dialect: Dialect, parse: Parse[U]): U = {
      parse(convert(inputLike))
    }
  }
}
```

Let's translate this into English. It is possible to parse a `T` into a `U`, if: 1) a `T` is convertible to `Input` (a scala.meta abstraction that by default knows how to encapsulate strings, streams and files), 2) if there's a correct dialect in scope, 3) if `U` is something that can be parsed into. In our case, we parse a Stream that scala.meta already supports into a Source, which is a representation of top-level code that scala.meta also supports.

### Exploring the parsed tree

The simplest way of examining the tree that we have is prettyprinting its syntax (`.show[Syntax]` or simply `.toString`). If you already have experience with other metaprogramming frameworks in Scala, you will notice something unusual: the prettyprint contains formatting and comments - source elements that are typically thrown away.

```scala
println(tree.show[Syntax])
```

```
00:14 ~/Projects/tutorial (view-bounds)$ sbt run
[info] Set current project to tutorial (in build file:/Users/xeno_by/Projects/tutorial/)
[info] Compiling 1 Scala source to /Users/xeno_by/Projects/tutorial/target/scala-2.11/classes...
[info] Running Test
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

In scala.meta, trees that are created by parsing fully remember the sources that they are parsed from, storing the details about these sources in their `.tokens` collections. Note how tokens not only cover everything, but also feature offsets of beginning and end, providing precise positions to trees that encapsulate them.

```scala
println(tree.tokens)
```

```
00:25 ~/Projects/tutorial (view-bounds)$ sbt run
[info] Set current project to tutorial (in build file:/Users/xeno_by/Projects/tutorial/)
[info] Compiling 1 Scala source to /Users/xeno_by/Projects/tutorial/target/scala-2.11/classes...
[info] Running Test
Slice(Tokenized(Input.Stream(<input stream>), Scala211, Vector(BOF (0..0), ...,
// Skipping some code from scala/math/Ordering.scala (114..166), \n (166..167), \n (167..168),
trait (168..173),   (173..174), LowPriorityOrderingImplicits (174..202), ...))
```

It is usually unnecessary to work at the level of tokens, because scala.meta trees are designed to express the overwhelming majority of language features simply via their structure. Let's see how view bounds are expressed in abstract syntax.

```scala
println(tree.show[Structure])
```

```
00:32 ~/Projects/tutorial (view-bounds)$ sbt run
[info] Set current project to tutorial (in build file:/Users/xeno_by/Projects/tutorial/)
[info] Compiling 1 Scala source to /Users/xeno_by/Projects/tutorial/target/scala-2.11/classes...
[info] Running Test
Source(Seq(
  Pkg(Term.Name("scala"),
    Pkg(Term.Name("math"),
      ...
        Defn.Def(
          Seq(Mod.Implicit()),
          Term.Name("ordered"),
          Seq(Type.Param(
            Nil,
            Type.Name("A"),
            Nil,
            Type.Bounds(None, None),
            Seq(Type.Apply(Type.Name("Comparable"), Seq(Type.Name("A")))),
            Nil)),
          Nil,
          Some(Type.Apply(Type.Name("Ordering"), Seq(Type.Name("A")))),
          ...))))))
```

With `.show[Structure]`, it is possible to see that type parameters have a dedicated field that store view bounds, and this is something that we will be targetting in this guide. Data constructors referred by the printouts correspond to AST nodes defined in [Trees.scala](/scalameta/trees/src/main/scala/scala/meta/Trees.scala).

However, if we take a closer look at Trees.scala, we'll notice that all the definitions of AST nodes live in the `scala.meta.internal.ast` package, which hints at the fact that there is a better way to manipulate trees. And sure there is - quasiquotes, as defined in [quasiquotes.md](/docs/quasiquotes.md). Knowing how to figure out and use internal structure of ASTs might come in handy in dire situations, but the main way of tree manipulations is using quasiquotes.

### Rewriting the parsed tree

In order to apply changes to the parsed tree, we can use the `Tree.transform` method of the collection-like UI to abstract syntax trees. In comparison with the traditional visitor approach, it features functional interface and requires minimal boilerplate. Note how we avoid the usual `object needToThinkOfSmartName extends Transformer { override def transform(tree: Tree): Tree = ... }` business.

```scala
import scala.meta._
import scala.meta.dialects.Scala211

object Test {
  def main(args: Array[String]): Unit = {
    val stream = getClass.getResourceAsStream("Ordering.scala")
    val tree = stream.parse[Source]
    val tree1 = tree.transform {
      case m @ q"..$mods def $name[..$tparams](...$paramss): $tpeopt = $expr" =>
        var evidences: List[Term.Param] = Nil
        val tparams1 = tparams.map {
          case tparam"..$mods $name[..$tparams] >: $lo <: $hi <% ..$vbounds : ..$cbounds" =>
            val paramEvidences = vbounds.map(vbound => param"implicit ${Term.fresh("ev")}: $vbound")
            evidences ++= paramEvidences
            tparam"..$mods $name[..$tparams] >: $lo <: $hi : ..$cbounds"
        }
        val paramss1 = if (evidences.nonEmpty) paramss :+ evidences else paramss
        q"..$mods def $name[..$tparams1](...$paramss1): $tpeopt = $expr"
    }
    println(tree1)
  }
}
```

Let's run this simple transformer to make sure that it indeed solves the problem at hand. Note how the printout of the transformed code retains original formatting. Achieving this with `scala.tools.nsc` and `scala.reflect` is currently prohibitively complex, because source-preserving rewritings in these frameworks can't be performed on the level of abstract syntax trees and must be done manually on the level of strings.

```
15:09 ~/Projects/tutorial (view-bounds)$ sbt run
[info] Set current project to tutorial (in build file:/Users/xeno_by/Projects/tutorial/)
[info] Compiling 1 Scala source to /Users/xeno_by/Projects/tutorial/target/scala-2.11/classes...
[info] Running Test
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
  implicit def ordered[A](implicit ev1: Comparable[A]): Ordering[A] = new Ordering[A] {
    def compare(x: A, y: A): Int = x compareTo y
  }
}

// Skipping some more code from scala/math/Ordering.scala
```

When doing code transformations, one should always think about hygiene, which entails two things: 1) making sure that synthetic references don't get captured by user-defined definitions (referential transparency), 2) making sure that synthetic definitions don't capture user-defined references (hygiene proper).

This tutorial is intentionally simplistic in the regard of hygiene, because the problem lends itself well to relaxed code generation. First of all, we don't have to worry about `=>` being unexpectedly captured by user definitions, because `=>` is a keyword, not a name. Secondly, avoiding violations of hygiene proper is easily achieved by a low-tech gensym-style solution (`Term.fresh`). Generating both unique and user-readable names (not `ev$1`, but something more along the lines of `ev`) is a completely different story and is left for future work.

### Going further

Even though `Tree.transform` feels simple, there is hidden depth behind it. In fact, collection-like APIs like `Tree.transform` and `Tree.collect` are just particular cases of the general tree query language (TQL) designed and implemented by [@begeric](https://github.com/begeric). In TQL, tree traversals not only transform trees, but also accumulate monoidal state. Let's make use of this by importing `scala.meta.tql._` and utilizing the `withResult` helper method.

```scala
import scala.meta._
import scala.meta.dialects.Scala211
import scala.meta.tql._

object Test {
  def main(args: Array[String]): Unit = {
    val stream = getClass.getResourceAsStream("Ordering.scala")
    val tree = stream.parse[Source]
    val tree1 = tree.transform {
      case m @ q"..$mods def $name[..$tparams](...$paramss): $tpeopt = $expr" =>
        val (tparams1, evidences) = tparams.transform {
          case tparam"..$mods $name[..$tparams] >: $lo <: $hi <% ..$vbounds : ..$cbounds" =>
            val evidences = vbounds.map(vbound => param"implicit ${Term.fresh("ev")}: $vbound").toList
            tparam"..$mods $name[..$tparams] >: $lo <: $hi : ..$cbounds" withResult evidences
        }
        val paramss1 = if (evidences.nonEmpty) paramss :+ evidences else paramss
        q"..$mods def $name[..$tparams1](...$paramss1): $tpeopt = $expr"
    }
    println(tree1)
  }
}
```

Thanks to TQL, we've been able to get rid of the pesky mutable state and make our transformer more declarative. If you're interested in learning more about TQL, you can find some docs at [begeric/TQL-scalameta](https://github.com/begeric/TQL-scalameta/wiki) as well as the accompanying technical report at [infoscience.epfl.ch](http://infoscience.epfl.ch/record/204789).


