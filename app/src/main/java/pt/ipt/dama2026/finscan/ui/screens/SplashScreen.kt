package pt.ipt.dama2026.finscan.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import pt.ipt.dama2026.finscan.R
import pt.ipt.dama2026.finscan.ui.theme.AmberAlert
import pt.ipt.dama2026.finscan.ui.theme.EmeraldGreen
import pt.ipt.dama2026.finscan.ui.theme.IndigoTechnological
import pt.ipt.dama2026.finscan.ui.theme.OffWhite
import pt.ipt.dama2026.finscan.ui.theme.SlateDark

@Composable
fun SplashScreen(onNavigateToHome: () -> Unit = {}) {
    LaunchedEffect(Unit) {
        // Splash Screen Delay
        kotlinx.coroutines.delay(3000)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        OffWhite,
                        Color(0xFFE0E7FF)
                    )
                )
            )
    ) {
        // Background Floating Particles
        FloatingParticles()

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo animado
            AnimatedLogo()

            // Blank Space
            Box(modifier = Modifier.size(40.dp))

            // Text
            Text(
                text = "Fin",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = SlateDark,
                modifier = Modifier.offset(y = (-5).dp)
            )
            Text(
                text = "Scan",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = IndigoTechnological,
                modifier = Modifier.offset(y = (-10).dp)
            )

            // Tagline
            Text(
                text = "Digitaliza os teus recibos com facilidade",
                fontSize = 16.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.offset(y = 10.dp)
            )

            // Loading dots
            Box(modifier = Modifier.size(40.dp))
            LoadingDots()

            // Texto de carregamento
            Text(
                text = "A preparar...",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.offset(y = 15.dp)
            )
        }

        // Barra de progresso no fundo
        ProgressBar(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun FloatingParticles() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    val particle1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -40f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 6000
                0f at 0
                20f at 1500
                40f at 3000
                20f at 4500
                0f at 6000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "particle1"
    )

    val particle2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -40f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 6000
                0f at 1000
                20f at 2500
                40f at 4000
                20f at 5500
                0f at 6000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "particle2"
    )

    val particle3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -40f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 6000
                0f at 2000
                20f at 3500
                40f at 5000
                20f at 6500 - 2000
                0f at 6000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "particle3"
    )

    // Partícula 1 - Indigo
    Box(
        modifier = Modifier
            .offset(y = particle1Offset.dp)
            .offset(x = 20.dp, y = 50.dp)
            .size(100.dp)
            .clip(CircleShape)
            .background(
                color = IndigoTechnological.copy(alpha = 0.1f)
            )
    )

    // Partícula 2 - Verde
    Box(
        modifier = Modifier
            .offset(y = particle2Offset.dp)
            .offset(x = (-20).dp, y = 500.dp)
            .size(150.dp)
            .clip(CircleShape)
            .background(
                color = EmeraldGreen.copy(alpha = 0.1f)
            )
    )

    // Partícula 3 - Âmbar
    Box(
        modifier = Modifier
            .offset(y = particle3Offset.dp)
            .offset(x = 30.dp, y = 350.dp)
            .size(80.dp)
            .clip(CircleShape)
            .background(
                color = AmberAlert.copy(alpha = 0.1f)
            )
    )
}

@Composable
fun AnimatedLogo() {
    val infiniteTransition = rememberInfiniteTransition(label = "logo")

    val pulseShadow by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 70f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shadow"
    )

    val scaleAnimation = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        scaleAnimation.animateTo(
            targetValue = 1f,
            animationSpec = tween(800, delayMillis = 300, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .size(140.dp)
            .shadow(
                elevation = pulseShadow.dp,
                shape = RoundedCornerShape(35.dp),
                ambientColor = IndigoTechnological.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(35.dp))
            .background(IndigoTechnological),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.playstore_icon),
            contentDescription = "FinScan Logo",
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(30.dp))
        )
    }
}

@Composable
fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0f at 0
                10f at 400
                0f at 1400
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )

    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0f at 200
                10f at 600
                0f at 1400
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )

    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0f at 400
                10f at 800
                0f at 1400
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(IndigoTechnological)
                .offset(y = dot1Offset.dp)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(IndigoTechnological)
                .offset(y = dot2Offset.dp)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(IndigoTechnological)
                .offset(y = dot3Offset.dp)
        )
    }
}

@Composable
fun ProgressBar(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "progress")

    val progressFill by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progressFill"
    )

    Box(
        modifier = modifier
            .offset(y = (-40).dp)
            .size(width = 120.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(
                color = IndigoTechnological.copy(alpha = 0.2f)
            )
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = 120.dp * progressFill,
                    height = 4.dp
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            IndigoTechnological,
                            EmeraldGreen
                        )
                    )
                )
                .clip(RoundedCornerShape(2.dp))
        )
    }
}
