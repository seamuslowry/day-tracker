package seamuslowry.daytracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import seamuslowry.daytracker.R
import seamuslowry.daytracker.ui.screens.entry.EntryScreen
import seamuslowry.daytracker.ui.screens.report.ReportScreen
import seamuslowry.daytracker.ui.screens.settings.SettingsScreen
import java.time.LocalDate

sealed class Screen<DataType>(val identifier: String, private val defaultData: DataType?) {
    data object Entry : Screen<Long>("entry", LocalDate.now().toEpochDay()) {
        const val INITIAL_DATE = "initialDate"
        override fun route(data: Long?) = "$identifier?$INITIAL_DATE=${data ?: "{$INITIAL_DATE}"}"
    }
    data object Report : Screen<Unit>("report", Unit)
    data object Settings : Screen<Unit>("settings", Unit)

    open fun route(data: DataType? = defaultData) = identifier
}

data class NavBarData<T>(
    val screen: Screen<out T>,
    val icon: ImageVector,
    val text: String,
)

@Composable
fun Navigation(
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val navigableScreens = listOf(
        NavBarData(Screen.Entry, Icons.AutoMirrored.Filled.Assignment, stringResource(R.string.entry)),
        NavBarData(Screen.Report, Icons.Filled.DateRange, stringResource(R.string.report)),
        NavBarData(Screen.Settings, Icons.Filled.Settings, stringResource(R.string.settings)),
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                navigableScreens.map {
                    NavigationBarItem(
                        icon = { Icon(it.icon, contentDescription = it.text) },
                        selected = currentRoute.orEmpty().startsWith(it.screen.identifier),
                        onClick = {
                            navController.navigate(it.screen.identifier) {
                                popUpTo(startDestination) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            modifier = Modifier.padding(paddingValues),
            navController = navController,
            startDestination = startDestination,
        ) {
            composable(
                Screen.Entry.route(null),
                arguments = listOf(
                    navArgument(Screen.Entry.INITIAL_DATE) {
                        type = NavType.LongType
                        defaultValue = LocalDate.now().toEpochDay()
                    },
                ),
            ) {
                EntryScreen()
            }
            composable(
                Screen.Report.route(),
            ) {
                ReportScreen(
                    onSelectDate = {
                        navController.navigate(Screen.Entry.route(it.toEpochDay()))
                    },
                )
            }
            composable(
                Screen.Settings.route(),
            ) {
                SettingsScreen()
            }
        }
    }
}
