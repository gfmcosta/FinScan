package pt.ipt.dama2026.finscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ipt.dama2026.finscan.ui.theme.*
import pt.ipt.dama2026.finscan.R


@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_label),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = SlateDark,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Profile Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* Handle click */ }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(SettingsProfilePlaceholderColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = SettingsIconTintColor
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                // TODO: Remove in PROD
                Text(
                    text = "Costa",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateDark
                )
                Text(
                    text = stringResource(R.string.settings_edit_user_label),
                    fontSize = 14.sp,
                    color = SettingsSubtextColor
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = SettingsSubtextColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dark Mode
        SettingsSwitchItem(
            icon = Icons.Default.NightsStay,
            iconContainerColor = SettingsDarkModeColor,
            label = stringResource(R.string.settings_dark_mode),
            initialValue = false
        )

        SettingsSectionHeader(stringResource(R.string.settings_profile_label))
        SettingsClickableItem(
            icon = Icons.Default.Person,
            iconContainerColor = SettingsEditProfileColor,
            label = stringResource(R.string.settings_edit_profile)
        )
        SettingsClickableItem(
            icon = Icons.Default.Lock,
            iconContainerColor = SettingsChangePasswordColor,
            label = stringResource(R.string.settings_change_password)
        )

        SettingsSectionHeader(stringResource(R.string.settings_notifications_label))
        SettingsSwitchItem(
            icon = Icons.Default.Notifications,
            iconContainerColor = SettingsNotificationsColor,
            label = stringResource(R.string.settings_notifications_label),
            initialValue = false
        )

        SettingsSectionHeader(stringResource(R.string.settings_general_label))
        SettingsClickableItem(
            icon = Icons.Default.Public,
            iconContainerColor = SettingsLanguageColor,
            label = stringResource(R.string.settings_language_label)
        )
        SettingsClickableItem(
            icon = Icons.AutoMirrored.Filled.Logout,
            iconContainerColor = SettingsLogoutColor,
            label = stringResource(R.string.settings_logout_label)
        )
        SettingsClickableItem(
            icon = Icons.Default.Info,
            iconContainerColor = SettingsAboutUsColor,
            label = stringResource(R.string.settings_about_us_label)
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = SlateDark,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsClickableItem(
    icon: ImageVector,
    iconContainerColor: Color,
    label: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = SettingsIconTintColor
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            color = SlateDark
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SettingsArrowColor
        )
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    iconContainerColor: Color,
    label: String,
    initialValue: Boolean
) {
    var checked by remember { mutableStateOf(initialValue) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = SettingsIconTintColor
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            color = SlateDark
        )
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = SettingsIconTintColor,
                checkedTrackColor = EmeraldGreen,
                uncheckedThumbColor = SettingsIconTintColor,
                uncheckedTrackColor = SettingsArrowColor,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    FinScanTheme {
        SettingsScreen()
    }
}
