fun main() {
    println("Enter regular expression:")
    val text = readLine() ?: ""
    val lexer = Lexer(text)
    lexer.tokenize()

    println()
    lexer.tokens.forEach {
        println("${it.type}, ${it.value}")
    }

    val parser = Parser(lexer.tokens)
    val node = parser.parse()

    val cfg = CFG(parser.ast, parser.initGroup, 1, 1, 1, mutableMapOf(), mutableMapOf())
    cfg.create(node, "S")

    println()

    for ((nt, rhsList) in cfg.rules) {
        for (rhs in rhsList) {
            val rhsStr = if (rhs.isEmpty()) "ε" else rhs.joinToString(" ")
            println("$nt -> $rhsStr")
        }
    }
}

//    +:
//    (aa|bb)(?1)
//    (a|(bb))(a|(?2))
//    (a|(b|c))d
//    ((a|b)c)*
//    (a)(?1)(a|(b|c))
//    (a*|(?:b|c))d
//    (a|(?2)b)(a(?1))
//    ((a|b)(?1))(/1|b)*
//    (aaaaa(?1))
//
//
//    -:
//    (a)*(/1)*
//    (a(?2)b|c)((?1)(/1))
//    ((a(?1)b|c)|(a|b))((?3)(/2))
//    (a|(?2)b)(a/1)
//    (a|(?2))(a|(bb/4))(a)
//    (a|(?2))(a|(bb/1))
//    a|b)
//    ((a)(b)(c)(d)(e)(f)(g)(h)(i)(j))
//    (a)(?2)
//    (a|(bb))(a|/2)
//    (?:aab)(?1)
//    ((abbb)|(baaa))(?2)(?1)/2
//    (aaaa/1)
//    (a)*/1