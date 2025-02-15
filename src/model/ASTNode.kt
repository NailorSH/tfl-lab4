package model

sealed interface ASTNode {
    data class Group(val id: Int, val node: ASTNode) : ASTNode
    data class RefStr(val id: Int) : ASTNode
    data class RefGroup(val id: Int, val inGroups: List<Int>) : ASTNode
    data class Star(val node: ASTNode, val starGroups: List<Int>) : ASTNode
    data class NonGroup(val node: ASTNode) : ASTNode
    data class Char(val char: String) : ASTNode
    data class Alter(val nodes: List<ASTNode>) : ASTNode
    data class Union(val nodes: List<ASTNode>) : ASTNode
}