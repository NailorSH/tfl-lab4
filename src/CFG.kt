import model.ASTNode

class CFG(
    val ast: LinkedHashMap<Int, ASTNode>,
    val initGroup: MutableList<Int>,
    var C: Int,
    var NG: Int,
    var S: Int,
    val rules: MutableMap<String, MutableList<List<String>>>,
    val N: MutableMap<Int, String>
) {
    fun create(node: ASTNode, start: String) {
        val N = cfg(node, null)
        rules[start] = mutableListOf(listOf(N))
    }

    fun cfg(node: ASTNode, start: String?): String {
        return when (node) {
            is ASTNode.Char -> {
                val N = start ?: "CH$C"
                C++
                rules.computeIfAbsent(N) { mutableListOf() }
                rules[N]!!.add(listOf(node.char))
                N
            }

            is ASTNode.RefStr -> {
                val strId = node.id
                if (!ast.containsKey(strId)) throw IllegalArgumentException("Reference to non-existent string /$strId")
                if (initGroup.contains(strId)) throw IllegalArgumentException("Reference to uninitialized string /$strId")

                if (!N.containsKey(strId)) {
                    N[strId] = "RF$strId"
                    // Строим правила для группы refId
                    val subN = cfg(ast[strId]!!, null)
                    val N = this.N[strId]!!
                    rules.computeIfAbsent(N) { mutableListOf() }
                    rules[N]!!.add(listOf(subN))
                }
                N[strId]!!
            }

            is ASTNode.RefGroup -> {
                val strId = node.id
                if (!N.containsKey(strId)) {
                    N[strId] = "GR$strId"
                    if (!ast.containsKey(strId)) throw IllegalArgumentException("Reference to non-existent group")

                    val subN = cfg(ast[strId]!!, null)
                    val N = this.N[strId]!!
                    rules.computeIfAbsent(N) { mutableListOf() }
                    rules[N]!!.add(listOf(subN))
                }
                N[strId]!!
            }

            is ASTNode.Group -> {
                var N = this.N[node.id]
                if (N == null) {
                    N = "G${node.id}"
                    this.N[node.id] = N
                }
                val subN = cfg(node.node, null)
                rules.computeIfAbsent(N) { mutableListOf() }
                rules[N]!!.add(listOf(subN))
                N
            }

            is ASTNode.NonGroup -> {
                // Генерируем новый нетерминал для незахватывающей группы
                val N = start ?: "N$NG"
                NG++
                val subN = cfg(node.node, null)
                rules.computeIfAbsent(N) { mutableListOf() }
                rules[N]!!.add(listOf(subN))
                N
            }

            is ASTNode.Star -> {
                // Звёздочка: X* означает 0 или более повторений X
                // Создаём нетерминал для звёздочки
                val N = start ?: "ST$S"
                S++
                val subN = cfg(node.node, null)
                // R -> ε | R subNt
                rules.computeIfAbsent(N) { mutableListOf() }
                rules[N]!!.add(listOf(subN))
                rules[N]!!.add(listOf(N, subN, "| ε"))
                N
            }

            is ASTNode.Union -> {
                val N = start ?: "C${NG + C}"
                NG++

                val seqNs = node.nodes.map { cfg(it, null) }
                rules.computeIfAbsent(N) { mutableListOf() }
                rules[N]!!.add(seqNs)
                N
            }

            is ASTNode.Alter -> {
                val N = start ?: "A${NG + C}"
                NG++

                // Альтернатива: для каждой ветви генерируем правило
                node.nodes.forEach { nod ->
                    val brN = cfg(nod, null)
                    rules.computeIfAbsent(N) { mutableListOf() }
                    rules[N]!!.add(listOf(brN))
                }
                N
            }

            else -> throw IllegalArgumentException("Unknown AST node type when checking references")
        }
    }
}