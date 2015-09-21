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