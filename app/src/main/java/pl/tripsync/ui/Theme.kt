package pl.tripsync.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

val Navy = Color(0xFF142C46)
val Teal = Color(0xFF007B78)
val Mint = Color(0xFF57D9CC)
val Warm = Color(0xFFF0B977)
private val Light = lightColorScheme(primary=Teal, onPrimary=Color.White, primaryContainer=Color(0xFFDAF4EF), onPrimaryContainer=Navy, secondary=Navy, secondaryContainer=Color(0xFFDAF4EF), onSecondaryContainer=Navy, onBackground=Navy, background=Color(0xFFF5F7F8), surface=Color.White, surfaceVariant=Color(0xFFE9EFF2), onSurface=Navy, onSurfaceVariant=Color(0xFF4A5C6D), outline=Color(0xFF6B7E8D))
private val Dark = darkColorScheme(primary=Mint, onPrimary=Navy, primaryContainer=Color(0xFF194743), onPrimaryContainer=Color(0xFFDAF4EF), secondary=Color(0xFFBBCEE5), secondaryContainer=Color(0xFF194743), onSecondaryContainer=Mint, background=Color(0xFF101C29), surface=Color(0xFF182A3C), surfaceVariant=Color(0xFF283C50), onSurface=Color(0xFFE8F0F7), onSurfaceVariant=Color(0xFFB4C5D2))

@Composable
fun TripTheme(dark: Boolean, content: @Composable () -> Unit) { MaterialTheme(colorScheme=if(dark) Dark else Light, content=content) }

@Composable
fun RouteLogo(modifier: Modifier = Modifier, color: Color = Teal) {
    Canvas(modifier.size(38.dp)) {
        val points=listOf(Offset(size.width*.12f,size.height*.66f),Offset(size.width*.38f,size.height*.22f),Offset(size.width*.63f,size.height*.75f),Offset(size.width*.90f,size.height*.31f))
        val path=Path().apply { moveTo(points[0].x,points[0].y); points.drop(1).forEach { lineTo(it.x,it.y) } }
        drawPath(path,color,style=Stroke(width=3.dp.toPx()))
        points.take(3).forEach { drawCircle(color,4.dp.toPx(),it) }
        drawLine(color,points.last(),Offset(size.width*.7f,size.height*.31f),3.dp.toPx())
        drawLine(color,points.last(),Offset(size.width*.9f,size.height*.51f),3.dp.toPx())
    }
}
