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