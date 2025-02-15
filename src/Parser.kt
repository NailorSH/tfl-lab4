import model.ASTNode
import model.Token
import model.TokenType

class Parser(val tokens: List<Token>) {
    var position = 0
    val initGroup = mutableListOf<Int>()
    val openBracket = mutableListOf<Int>()
    var groups = 0
    val maxGroups = 9
    val ast = LinkedHashMap<Int, ASTNode>()
    val refStr = mutableMapOf<Int, List<Int>>()

    fun get(): Token? = tokens.getOrNull(position)

    fun check(type: TokenType?): Token {
        val token = get() ?: throw IllegalArgumentException("Unexpected end of expression")
        if (type != null && token.type != type) {
            throw IllegalArgumentException("Expected $type, found ${token.type}")
        }
        position++
        return token
    }

    fun parse(): ASTNode {
        val node = altParser()
        if (get() != null) throw IllegalArgumentException("Extra characters after valid expression")

        parseNodes(node, mutableSetOf(), false, false)

        return node
    }

    fun altParser(): ASTNode {
        val nodes = mutableListOf<ASTNode>()
        nodes.add(unionParse())
        while (get() != null && get()?.type == TokenType.ALTER) {
            check(TokenType.ALTER)
            if(get() == null || get()?.type in listOf(TokenType.ALTER, TokenType.CLOSE)) {
                throw IllegalArgumentException("Unexpected end of an expression")
            }
            nodes.add(unionParse())
        }
        return if (nodes.size == 1) nodes[0] else ASTNode.Alter(nodes)
    }

    fun unionParse(): ASTNode {
        val nodes = mutableListOf<ASTNode>()
        while (get() != null && get()?.type !in listOf(TokenType.ALTER, TokenType.CLOSE)) {
            nodes.add(starParse())
        }
        return if (nodes.size == 1) nodes[0] else ASTNode.Union(nodes)
    }

    fun starParse(): ASTNode {
        var node = bracketParse()
        while (get()!= null && get()?.type == TokenType.STAR) {
            check(TokenType.STAR)
            node = ASTNode.Star(node, emptyList())
        }
        return node
    }

    fun bracketParse(): ASTNode {
        val token = get() ?: throw IllegalArgumentException(
            "Unexpected end of expression while expecting base expression"
        )
        return when (token.type) {
            TokenType.GROUP_OPEN -> {
                check(TokenType.GROUP_OPEN)
                groups++
                if (groups > maxGroups) throw IllegalArgumentException("Number of groups $groups exceeded max")
                val id = groups
                openBracket.add(groups)
                val node = altParser()
                check(TokenType.CLOSE)
                openBracket.removeLast()
                ast[id] = node
                ASTNode.Group(id, node)
            }

            TokenType.NON_GROUP_OPEN -> {
                check(TokenType.NON_GROUP_OPEN)
                val node = altParser()
                check(TokenType.CLOSE)
                ASTNode.NonGroup(node)
            }

            TokenType.REF_GROUP_OPEN -> {
                check(TokenType.REF_GROUP_OPEN)
                check(TokenType.CLOSE)
                ASTNode.RefGroup(token.value as Int, openBracket.toList())
            }

            TokenType.CHAR -> {
                check(TokenType.CHAR)
                ASTNode.Char(token.value as String)
            }

            TokenType.REF_STR -> {
                val value = token.value as Int
                if (value in openBracket) throw IllegalArgumentException("Reference /$value not fully processed yet")

                check(TokenType.REF_STR)
                refStr.getOrPut(value) { emptyList() }
                refStr[value] = refStr[value]!! + openBracket
                ASTNode.RefStr(value)
            }

            else -> throw IllegalArgumentException("Invalid token: $token")
        }
    }

    fun parseNodes(
        node: ASTNode,
        definedGroups: MutableSet<String>,
        inAlt: Boolean,
        inStar: Boolean
    ): MutableSet<String> {
        when (node) {
            is ASTNode.Char, is ASTNode.RefStr -> return definedGroups

            is ASTNode.RefGroup -> {
                checkIsInit(node)
                return definedGroups
            }

            is ASTNode.Group -> {
                if (inAlt || inStar) initGroup.add(node.id)
                val newDefinedGroups = parseNodes(node.node, definedGroups, inAlt, inStar)
                newDefinedGroups.add(node.id.toString())
                return newDefinedGroups
            }

            is ASTNode.NonGroup -> return parseNodes(node.node, definedGroups, inAlt, inStar)

            is ASTNode.Star -> {
                var tempInStar = true
                val result = parseNodes(node.node, definedGroups, inAlt, tempInStar)
                tempInStar = false
                return result
            }

            is ASTNode.Union -> {
                var currentDefined = definedGroups
                for (child in node.nodes) {
                    currentDefined = parseNodes(child, currentDefined, inAlt, inStar)
                }
                return currentDefined
            }

            is ASTNode.Alter -> {
                val allSides = mutableSetOf<String>()
                for (child in node.nodes) {
                    val tempInAlt = true
                    val childSides = parseNodes(child, definedGroups, tempInAlt, inStar)
                    allSides.addAll(childSides)
                }
                return allSides
            }

            else -> throw IllegalArgumentException("Unknown AST node type when checking links")
        }
    }

    fun checkIsInit(node: ASTNode.RefGroup): Boolean {
        if (refStr.isEmpty()) return false
        for ((key, value) in refStr) {
            if (node.id in value && value.isNotEmpty()) {
                throw IllegalStateException("Link to an uninitialized group /$key")
            }
        }
        return false
    }
}