import model.Token
import model.TokenType

class Lexer(val text: String) {
    val tokens = mutableListOf<Token>()

    fun tokenize() {
        var position = 0
        while (position < text.length) {
            val char = text.getOrNull(position) ?: break

            when {
                char.isWhitespace() -> position++
                char.isLetter() -> {
                    position++
                    tokens.add(Token(TokenType.CHAR, char.toString()))
                }

                char == '(' -> {
                    position++
                    val next = text.getOrNull(position)
                    when (next) {
                        '?' -> {
                            position++
                            val next2 = text.getOrNull(position)
                            when (next2) {
                                ':' -> {
                                    position++
                                    tokens.add(Token(TokenType.NON_GROUP_OPEN, null))
                                }

                                in '1'..'9' -> {
                                    position++
                                    tokens.add(Token(TokenType.REF_GROUP_OPEN, next2.toString().toInt()))
                                }

                                else -> throw IllegalArgumentException("Unexpected character after ?: expected ':' or number, got $next2")
                            }
                        }

                        else -> tokens.add(Token(TokenType.GROUP_OPEN, null))
                    }
                }

                char == ')' -> {
                    position++
                    tokens.add(Token(TokenType.CLOSE, null))
                }

                char == '*' -> {
                    position++
                    tokens.add(Token(TokenType.STAR, null))
                }

                char == '/' -> {
                    position++
                    val next = text.getOrNull(position)
                    if (next in '1'..'9') {
                        position++
                        tokens.add(Token(TokenType.REF_STR, next.toString().toInt()))
                    } else {
                        throw IllegalArgumentException("Unexpected character after / expected number, got $next")
                    }
                }

                char == '|' -> {
                    position++
                    tokens.add(Token(TokenType.ALTER, null))
                }

                else -> throw IllegalArgumentException("Unknown character $char")
            }
        }
    }
}