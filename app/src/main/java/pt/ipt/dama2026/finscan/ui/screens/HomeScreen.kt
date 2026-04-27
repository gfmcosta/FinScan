package pt.ipt.dama2026.finscan.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import pt.ipt.dama2026.finscan.R
import pt.ipt.dama2026.finscan.ui.theme.*

/**
 * Class to define the bottom navbar routes, icons and text.
 * @param route Defined route to a specific nav item
 * @param icon Defined icon to a specific nav item
 * @param labelRes: Defined text label to a specific nav item
 */
sealed class BottomNavItem(val route: String, val icon: ImageVector, val labelRes: Int) {
    object Home : BottomNavItem("home", Icons.Default.Home, R.string.home_label)
    object Receipts : BottomNavItem("receipts", Icons.Default.Receipt, R.string.receipts_label)
    object Scan : BottomNavItem("scan", Icons.Default.QrCodeScanner, R.string.scan_label)
    object Reports : BottomNavItem("reports", Icons.Default.Timeline, R.string.reports_label)
    object Settings : BottomNavItem("settings", Icons.Default.Settings, R.string.settings_label)
}


@Composable
fun MainScreen() {
    // object to control what item is selected (nav item)
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Receipts,
        BottomNavItem.Scan,
        BottomNavItem.Reports,
        BottomNavItem.Settings,
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                items.forEachIndexed { index, item ->
                    val label = stringResource(id = item.labelRes)
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndigoTechnological,
                            selectedTextColor = IndigoTechnological,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = IndigoTechnological.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        // Function responsible to redirect to the different's screens
        // TODO: Change in PROD
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedItem) {
                0 -> HomeScreen()
                1 -> null
                2 -> null
                3 -> null
                4 -> null
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val userName = "Costa" // TODO: Change in PROD
    val currentDate = remember { 
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) 
    }
    val monthlySpent = "450,25" //TODO: Change in PROD

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_hi_title) + userName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateDark
                )
                Text(
                    text = currentDate,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            // User Avatar placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(IndigoTechnological.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = IndigoTechnological)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Monthly Spent Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = IndigoTechnological)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(IndigoTechnological, Color(0xFF818CF8))
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.home_monthly_spent_title),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp
                    )
                    Text(
                        text = "$monthlySpent€",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Chart Section
        Text(
            text = stringResource(R.string.home_category_spent_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = SlateDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Simple Line Chart using Canvas
                LineChartPlaceholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Top 5 most spent categories
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    //TODO: Remove in PROD
                    CategoryLegendItem("A", EmeraldGreen)
                    CategoryLegendItem("B", IndigoTechnological)
                    CategoryLegendItem("C", AmberAlert)
                    CategoryLegendItem("D", Color.Magenta)
                    CategoryLegendItem("E", Color.Cyan)
                }
            }
        }
    }
}

@Composable
fun LineChartPlaceholder(modifier: Modifier = Modifier) {
    val chartColor = IndigoTechnological
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val points = listOf(0.2f, 0.5f, 0.3f, 0.6f, 0.4f, 0.8f, 0.7f, 0.9f)
        val path = Path()
        val fillPath = Path()

        val stepX = width / (points.size - 1)

        points.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - (value * height * 0.8f) - (height * 0.1f) // Padding top/bottom
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (index == points.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        // Draw fill gradient
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(chartColor.copy(alpha = 0.3f), Color.Transparent)
            )
        )

        // Draw line
        drawPath(
            path = path,
            color = chartColor,
            style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        // Draw points
        points.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - (value * height * 0.8f) - (height * 0.1f)
            drawCircle(
                color = chartColor,
                radius = 4.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}

@Composable
fun CategoryLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}
