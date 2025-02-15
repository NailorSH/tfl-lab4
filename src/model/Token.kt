package model

data class Token(
    val type: TokenType,
    val value: Any? = null
)