package com.Lia.assistant.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.Lia.assistant.license.AccessKeyManager
import com.Lia.assistant.voice.VoiceForegroundService
import com.Lia.assistant.voice.VoiceSessionManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.Lia.assistant.data.Prefs
import com.Lia.assistant.ui.components.BottomBarLift
import com.Lia.assistant.ui.components.EdgeGlowHost
import com.Lia.assistant.ui.components.LiaBottomBar
import com.Lia.assistant.ui.screens.ChatScreen
import com.Lia.assistant.ui.screens.DebugScreen
import com.Lia.assistant.ui.screens.HistoryScreen
import com.Lia.assistant.ui.screens.HomeScreen
import com.Lia.assistant.ui.screens.OnboardingScreen
import com.Lia.assistant.ui.screens.OrbStyleScreen
import com.Lia.assistant.ui.screens.PermissionsScreen
import com.Lia.assistant.ui.screens.PersonalityScreen
import com.Lia.assistant.ui.screens.PrivacyScreen
import com.Lia.assistant.ui.screens.AboutScreen
import com.Lia.assistant.ui.screens.ProfileScreen
import com.Lia.assistant.ui.screens.QuickActionsScreen
import com.Lia.assistant.ui.screens.SettingsScreen
import com.Lia.assistant.ui.screens.SetupScreen
import com.Lia.assistant.ui.screens.VoiceScreen
import com.Lia.assistant.ui.screens.forge.ForgeScreen
import com.Lia.assistant.forge.ForgeController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import com.Lia.assistant.ui.screens.splash.LiaSplash3D
import com.Lia.assistant.ui.screens.splash.SplashSession
import com.Lia.assistant.ui.theme.LiaTheme
import com.Lia.assistant.ui.theme.LocalBottomBarInset
import com.Lia.assistant.ui.theme.nightSky
import androidx.compose.runtime.saveable.rememberSaveable

/** Root of the app: splash, theme, navigation and the floating bottom bar. */
@Composable
fun LiaApp() {
    val ctx = LocalContext.current
    remember { AppSettings.load(ctx) }
    remember { AccessKeyManager.refreshInfo(ctx) }
    remember { com.Lia.assistant.voice.MoodState.mood = Prefs.mood(ctx) }
    remember { if (Prefs.wakeOn(ctx)) com.Lia.assistant.wake.WakeControl.start(ctx) }
    remember { if (Prefs.alertsOn(ctx)) com.Lia.assistant.alerts.PhoneMonitorService.start(ctx) }
    val licenseScope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                licenseScope.launch {
                    AccessKeyManager.liveCheck(ctx)
                    if (!AccessKeyManager.isActive() && VoiceSessionManager.isActive.value) {
                        VoiceForegroundService.stop(ctx)
                    }
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    var splashing by rememberSaveable { mutableStateOf(!SplashSession.played) }
    val nav = rememberNavController()
    val start = remember {
        when {
            !Prefs.hasOnboarded(ctx) -> Routes.ONBOARDING
            !Prefs.hasApiKey(ctx) -> Routes.SETUP
            else -> Routes.HOME
        }
    }

    val go: (String) -> Unit = { route -> nav.navigate(route) { launchSingleTop = true } }
    val forgeOpen by ForgeController.pendingOpen.collectAsState()
    LaunchedEffect(forgeOpen) {
        if (forgeOpen) {
            ForgeController.consumeOpen()
            nav.navigate(Routes.FORGE) { launchSingleTop = true }
        }
    }
    val back: () -> Unit = { nav.popBackStack() }
    val switchTab: (String) -> Unit = { route ->
        nav.navigate(route) {
            popUpTo(Routes.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }

    LiaTheme(mode = AppSettings.themeMode, reducedMotion = AppSettings.reducedMotion) {
        val backStack by nav.currentBackStackEntryAsState()
        val route = backStack?.destination?.route?.substringBefore('?')
        val showBar = route != null && route in Routes.barRoutes

        Box(Modifier.fillMaxSize().nightSky()) {
            if (splashing) {
                LiaSplash3D(onFinished = { splashing = false })
            } else {
                CompositionLocalProvider(
                    LocalBottomBarInset provides if (showBar) BottomBarLift else 0.dp,
                ) {
                    NavHost(navController = nav, startDestination = start, modifier = Modifier.fillMaxSize()) {
                        composable(Routes.ONBOARDING) {
                            OnboardingScreen(onDone = {
                                Prefs.saveOnboarded(ctx)
                                val next = if (Prefs.hasApiKey(ctx)) Routes.HOME else Routes.SETUP
                                nav.navigate(next) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                            })
                        }
                        composable(Routes.SETUP) {
                            SetupScreen(onSaved = {
                                nav.navigate(Routes.HOME) { popUpTo(Routes.SETUP) { inclusive = true } }
                            })
                        }
                        composable(Routes.HOME) {
                            HomeScreen(
                                onTalk = { go(Routes.VOICE) },
                                onType = { go(Routes.chat()) },
                                onProfile = { go(Routes.PROFILE) },
                                onQuickActions = { go(Routes.QUICK_ACTIONS) },
                            )
                        }
                        composable(
                            Routes.CHAT_ROUTE,
                            arguments = listOf(navArgument("id") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }),
                        ) { entry ->
                            ChatScreen(conversationId = entry.arguments?.getString("id"), onBack = null)
                        }
                        composable(Routes.HISTORY) {
                            HistoryScreen(onOpen = { go(Routes.chat(it)) }, onNewChat = { go(Routes.chat()) })
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                onOpen = go,
                                onChangeKey = {
                                    nav.navigate(Routes.SETUP) { popUpTo(Routes.HOME) { inclusive = true } }
                                },
                            )
                        }
                        composable(Routes.VOICE) { VoiceScreen(onClose = back) }
                        composable(Routes.QUICK_ACTIONS) { QuickActionsScreen(onOpen = go) }
                        composable(Routes.PROFILE) { ProfileScreen(onBack = back) }
                        composable(Routes.PERSONALITY) { PersonalityScreen(onBack = back) }
                        composable(Routes.ORB_STYLE) { OrbStyleScreen(onBack = back) }
                        composable(Routes.PERMISSIONS) { PermissionsScreen(onBack = back) }
                        composable(Routes.PRIVACY) { PrivacyScreen(onBack = back) }
                        composable(Routes.ABOUT) { AboutScreen(onBack = back) }
                        composable(Routes.DEBUG) { DebugScreen(onBack = back) }
                        composable(Routes.FORGE) { ForgeScreen(onBack = back) }
                        composable(Routes.MONITOR) { com.Lia.assistant.ui.screens.MonitorScreen(onBack = back) }
                    }
                }

                if (showBar) {
                    LiaBottomBar(
                        current = route,
                        onNavigate = switchTab,
                        onTalk = { go(Routes.VOICE) },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
            EdgeGlowHost()
        }
    }
}
