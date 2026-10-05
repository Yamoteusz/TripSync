package pl.tripsync.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Navy = Color(0xFF122E38)
val Teal = Color(0xFF006B62)
val Mint = Color(0xFFB5E9CA)
val Warm = Color(0xFFF1BC79)
private val Light = lightColorScheme(primary=Teal,onPrimary=Color.White,primaryContainer=Color(0xFFDFF2E7),onPrimaryContainer=Navy,secondary=Navy,secondaryContainer=Color(0xFFECE8DB),onSecondaryContainer=Navy,background=Color(0xFFF5F4EF),onBackground=Navy,surface=Color(0xFFFFFEFA),onSurface=Navy,surfaceVariant=Color(0xFFEBEEE6),onSurfaceVariant=Color(0xFF54645E),outline=Color(0xFF71817B),outlineVariant=Color(0xFFDCE2D9))
private val Dark = darkColorScheme(primary=Mint,onPrimary=Navy,primaryContainer=Color(0xFF214A3D),onPrimaryContainer=Color(0xFFDFF2E7),secondary=Color(0xFFB8CDC4),secondaryContainer=Color(0xFF35493F),onSecondaryContainer=Mint,background=Color(0xFF111E1B),onBackground=Color(0xFFE6EEE7),surface=Color(0xFF192B25),onSurface=Color(0xFFE6EEE7),surfaceVariant=Color(0xFF2B3F35),onSurfaceVariant=Color(0xFFB5C5BA),outline=Color(0xFF81958A),outlineVariant=Color(0xFF3B5146))
private val Type = Typography(
    headlineLarge=TextStyle(fontWeight=FontWeight.Bold,fontSize=32.sp,lineHeight=38.sp,letterSpacing=(-1).sp),
    headlineMedium=TextStyle(fontWeight=FontWeight.Bold,fontSize=28.sp,lineHeight=34.sp,letterSpacing=(-0.7).sp),
    headlineSmall=TextStyle(fontWeight=FontWeight.Bold,fontSize=24.sp,lineHeight=30.sp,letterSpacing=(-0.4).sp),
    titleLarge=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=21.sp,lineHeight=28.sp),
    titleMedium=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=17.sp,lineHeight=24.sp),
    bodyLarge=TextStyle(fontSize=16.sp,lineHeight=24.sp),
    bodyMedium=TextStyle(fontSize=14.sp,lineHeight=21.sp),
    bodySmall=TextStyle(fontSize=12.sp,lineHeight=18.sp),
    labelLarge=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=14.sp,lineHeight=20.sp),
    labelMedium=TextStyle(fontWeight=FontWeight.Medium,fontSize=12.sp,lineHeight=18.sp)
)

@Composable
fun TripTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=if(dark) Dark else Light,typography=Type,shapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(18.dp),large=RoundedCornerShape(24.dp)),content=content)
}

@Composable
fun RouteLogo(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier.size(38.dp)) {
        val points=listOf(Offset(size.width*.15f,size.height*.72f),Offset(size.width*.39f,size.height*.24f),Offset(size.width*.64f,size.height*.70f),Offset(size.width*.87f,size.height*.28f))
        val path=Path().apply {moveTo(points[0].x,points[0].y);points.drop(1).forEach {lineTo(it.x,it.y)}}
        drawPath(path,color,style=Stroke(width=size.width*.085f,cap=StrokeCap.Round,join=StrokeJoin.Round))
        points.take(3).forEach {drawCircle(color,size.width*.075f,it)}
        drawLine(color,points.last(),Offset(size.width*.69f,size.height*.28f),size.width*.085f,cap=StrokeCap.Round)
        drawLine(color,points.last(),Offset(size.width*.87f,size.height*.46f),size.width*.085f,cap=StrokeCap.Round)
    }
}
