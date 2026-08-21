package com.example.superinvoice.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * O set de ícones do guia de estilo: somente contorno, jamais sólido.
 *
 * Traço 1,5px, terminações redondas, sem preenchimento. A cor vem do `tint`
 * do [androidx.compose.material3.Icon], então os paths são desenhados em
 * preto e tingidos na hora de compor.
 */
object InvIcons {

    // ---- Do guia ---------------------------------------------------------

    val Document: ImageVector by lazy {
        invIcon(
            "M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z",
            "M14 3v5h5",
            "M9 13h6",
            "M9 17h4"
        )
    }

    val Person: ImageVector by lazy {
        invIcon(
            circle(12f, 8f, 3.4f),
            "M5 20a7 7 0 0 1 14 0"
        )
    }

    val Gear: ImageVector by lazy {
        invIcon(
            circle(12f, 12f, 3.2f),
            "M12 3.5v2", "M12 18.5v2", "M3.5 12h2", "M18.5 12h2",
            "M6 6l1.5 1.5", "M16.5 16.5 18 18", "M18 6l-1.5 1.5", "M7.5 16.5 6 18"
        )
    }

    val Search: ImageVector by lazy {
        invIcon(
            circle(11f, 11f, 7f),
            "m20 20-3.5-3.5"
        )
    }

    val Plus: ImageVector by lazy {
        invIcon("M12 5v14", "M5 12h14")
    }

    val Close: ImageVector by lazy {
        invIcon("M18 6 6 18", "M6 6l12 12")
    }

    val ChevronRight: ImageVector by lazy {
        invIcon("m9 6 6 6-6 6")
    }

    val Check: ImageVector by lazy {
        invIcon("m5 12 5 5L19 7")
    }

    // ---- Derivados, na mesma linguagem -----------------------------------

    val Minus: ImageVector by lazy { invIcon("M5 12h14") }

    val Trash: ImageVector by lazy {
        invIcon(
            "M4 6.5h16",
            "M9.5 6.5v-2h5v2",
            "M6.5 6.5 7.5 20a1.5 1.5 0 0 0 1.5 1.5h6a1.5 1.5 0 0 0 1.5-1.5l1-13.5",
            "M10 10.5v7", "M14 10.5v7"
        )
    }

    val More: ImageVector by lazy {
        invIcon(
            circle(12f, 5f, 1.2f),
            circle(12f, 12f, 1.2f),
            circle(12f, 19f, 1.2f)
        )
    }

    val Phone: ImageVector by lazy {
        invIcon(
            "M7.5 2.5h9A1.5 1.5 0 0 1 18 4v16a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 6 20V4a1.5 1.5 0 0 1 1.5-1.5z",
            "M10.5 18.5h3"
        )
    }

    val Mail: ImageVector by lazy {
        invIcon(
            "M4.5 5.5h15A1.5 1.5 0 0 1 21 7v10a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 17V7a1.5 1.5 0 0 1 1.5-1.5z",
            "m3.5 7 8.5 6 8.5-6"
        )
    }

    val Home: ImageVector by lazy {
        invIcon(
            "m3.5 10.5 8.5-7 8.5 7",
            "M6 9.5V20a.5.5 0 0 0 .5.5h11a.5.5 0 0 0 .5-.5V9.5",
            "M10 20.5v-6h4v6"
        )
    }

    val Pencil: ImageVector by lazy {
        invIcon(
            "M4 20h4L18.5 9.5a2.1 2.1 0 0 0-3-3L5 17v3z",
            "m14.5 7.5 3 3"
        )
    }

    val Lock: ImageVector by lazy {
        invIcon(
            "M6.5 10.5h11a1 1 0 0 1 1 1v8a1 1 0 0 1-1 1h-11a1 1 0 0 1-1-1v-8a1 1 0 0 1 1-1z",
            "M8.5 10.5v-3a3.5 3.5 0 0 1 7 0v3"
        )
    }

    val Notes: ImageVector by lazy {
        invIcon("M5 7h14", "M5 12h14", "M5 17h9")
    }

    val Address: ImageVector by lazy {
        invIcon(
            "M12 21.5s7-6 7-11.5a7 7 0 1 0-14 0c0 5.5 7 11.5 7 11.5z",
            circle(12f, 10f, 2.6f)
        )
    }

    val Zip: ImageVector by lazy {
        invIcon(
            "M4.5 5.5h15A1.5 1.5 0 0 1 21 7v10a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 17V7a1.5 1.5 0 0 1 1.5-1.5z",
            "M10.5 9.5 9.5 14.5", "M14.5 9.5 13.5 14.5", "M8.5 11h7", "M8 13h7"
        )
    }

    val Currency: ImageVector by lazy {
        invIcon(
            "M16 7.6c-.8-1.6-2.3-2.4-4-2.4-2.2 0-3.8 1.2-3.8 3.1s1.6 2.8 3.9 3.3c2.5.5 4.1 1.4 4.1 3.4s-1.8 3.3-4.2 3.3c-1.9 0-3.5-.8-4.3-2.4",
            "M12 3.5v2.2", "M12 18.3v2.2"
        )
    }
}

/** Circunferência como path SVG — o [PathParser] não tem primitiva de círculo. */
private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${r * 2} 0a$r $r 0 1 0 ${-r * 2} 0"

private fun invIcon(vararg paths: String): ImageVector {
    val builder = ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
    paths.forEach { d ->
        builder.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
    return builder.build()
}
