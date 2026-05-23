package com.cuidavoz.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomDestinations = listOf(
    BottomDestination("home", "Inicio", Icons.Outlined.Home),
    BottomDestination("medications", "Pastillas", Icons.Outlined.LocalHospital),
    BottomDestination("history", "Historial", Icons.Outlined.History),
    BottomDestination("family", "Familia", Icons.Outlined.Favorite),
    BottomDestination("settings", "Ajustes", Icons.Outlined.Settings),
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar {
        bottomDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination.route) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                    )
                },
                label = {
                    Text(destination.label)
                },
            )
        }
    }
}
