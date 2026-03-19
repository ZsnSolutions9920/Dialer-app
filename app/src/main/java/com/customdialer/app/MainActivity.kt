package com.customdialer.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.customdialer.app.data.api.RetrofitClient
import com.customdialer.app.ui.MainViewModel
import com.customdialer.app.ui.navigation.*
import com.customdialer.app.ui.components.IncomingCallOverlay
import com.customdialer.app.ui.screens.*
import com.customdialer.app.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        handleDeepLink(intent)
        setContent {
            CustomDialerTheme {
                MainApp(viewModel = mainViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from browser (Stripe checkout)
        if (mainViewModel.isLoggedIn.value) {
            mainViewModel.refreshAfterPurchase()
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        val path = uri.path ?: ""
        val host = uri.host ?: ""
        if (path == "/success-purchase" || host == "purchase-success") {
            mainViewModel.handlePurchaseSuccess()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val loginLoading by viewModel.loginLoading.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val agent by viewModel.agent.collectAsStateWithLifecycle()

    // Call state
    val isOnCall by viewModel.isOnCall.collectAsStateWithLifecycle()
    val activeCallNumber by viewModel.activeCallNumber.collectAsStateWithLifecycle()
    val callStatus by viewModel.callStatus.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val voiceReady by viewModel.voiceReady.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Incoming call state
    val hasIncomingCall by viewModel.hasIncomingCall.collectAsStateWithLifecycle()
    val incomingCallFrom by viewModel.incomingCallFrom.collectAsStateWithLifecycle()
    val incomingCallerName by viewModel.incomingCallerName.collectAsStateWithLifecycle()

    val C = AppColors
    val isDark = ThemeState.isDarkMode.value

    // Permission handling
    var pendingDialNumber by remember { mutableStateOf<String?>(null) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingDialNumber != null) {
            viewModel.makeCall(pendingDialNumber!!)
            pendingDialNumber = null
        } else if (!granted) {
            pendingDialNumber = null
        }
    }

    // Request mic permission on login
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && !viewModel.twilioVoice.hasMicPermission()) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // VoIP dial function with permission check
    fun dialVoip(number: String) {
        if (viewModel.twilioVoice.hasMicPermission()) {
            viewModel.makeCall(number)
        } else {
            pendingDialNumber = number
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Package gating
    val custPackage by viewModel.customerPackage.collectAsStateWithLifecycle()

    // Which screens are unlocked per package
    fun isScreenUnlocked(route: String): Boolean {
        if (route in listOf(Screen.Dashboard.route, Screen.Pricing.route, Screen.Store.route, Screen.Profile.route, Screen.Settings.route)) return true
        return when (custPackage) {
            "basic" -> route in listOf(Screen.Dialer.route, Screen.CallHistory.route)
            "silver" -> route in listOf(Screen.Dialer.route, Screen.CallHistory.route, Screen.Contacts.route)
            "premium" -> true
            else -> false // "free" — nothing unlocked except dashboard/store/profile
        }
    }

    // Purchase success deep link state
    val showPurchaseSuccess by viewModel.showPurchaseSuccess.collectAsStateWithLifecycle()

    LaunchedEffect(showPurchaseSuccess) {
        if (showPurchaseSuccess && isLoggedIn) {
            navController.navigate(Screen.PurchaseSuccess.route) {
                popUpTo(Screen.Dashboard.route) { saveState = true }
                launchSingleTop = true
            }
        }
    }

    // Global checkout URL handler — opens Stripe in browser from any screen
    val globalCheckoutUrl by viewModel.checkoutUrl.collectAsStateWithLifecycle()
    val globalSetupCardUrl by viewModel.setupCardUrl.collectAsStateWithLifecycle()
    val globalContext = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(globalCheckoutUrl) {
        if (globalCheckoutUrl != null) {
            try {
                globalContext.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(globalCheckoutUrl)))
            } catch (_: Exception) { }
            viewModel.clearCheckoutUrl()
        }
    }

    LaunchedEffect(globalSetupCardUrl) {
        if (globalSetupCardUrl != null) {
            try {
                globalContext.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(globalSetupCardUrl)))
            } catch (_: Exception) { }
            viewModel.clearSetupCardUrl()
        }
    }

    // KYC state
    val kycStatus by viewModel.kycStatus.collectAsStateWithLifecycle()
    val kycLoading by viewModel.kycLoading.collectAsStateWithLifecycle()
    val kycError by viewModel.kycError.collectAsStateWithLifecycle()
    val kycMessage by viewModel.kycMessage.collectAsStateWithLifecycle()

    // Auth screen state
    var showSignup by remember { mutableStateOf(false) }

    if (!isLoggedIn) {
        if (showSignup) {
            SignupScreen(
                onSignup = { email, pass, name -> viewModel.signup(email, pass, name) },
                onNavigateToLogin = { showSignup = false },
                isLoading = loginLoading,
                error = loginError
            )
        } else {
            LoginScreen(
                onLogin = { email, pass -> viewModel.login(email, pass) },
                onNavigateToSignup = { showSignup = true },
                isLoading = loginLoading,
                error = loginError
            )
        }
    } else if (kycStatus == "checking") {
        // Show loading while checking KYC status
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(C.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = PrimaryBlue)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Checking verification status...", color = C.textSecondary, fontSize = 14.sp)
            }
        }
    } else if (kycStatus == "none" || kycStatus == "rejected") {
        // Show KYC form
        KycScreen(
            onSubmit = { name, cnicUri, selfieUri ->
                viewModel.submitKyc(name, cnicUri, selfieUri)
            },
            isLoading = kycLoading,
            error = kycError,
            rejectionMessage = if (kycStatus == "rejected") kycMessage else null,
            onLogout = { viewModel.logout() }
        )
    } else if (kycStatus == "pending") {
        // Show pending screen
        KycPendingScreen(
            onRefreshStatus = { viewModel.checkKycStatus() },
            onLogout = { viewModel.logout() },
            isChecking = kycStatus == "checking"
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = C.surface
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (viewModel.tokenManager.getCustomerName()
                                    ?: agent?.displayName
                                    ?: agent?.username
                                    ?: "?").take(1).uppercase(),
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                viewModel.tokenManager.getCustomerName()
                                    ?: agent?.displayName
                                    ?: agent?.username
                                    ?: "User",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = C.textPrimary
                            )
                            Text(
                                viewModel.tokenManager.getCustomerEmail() ?: "@${agent?.username ?: ""}",
                                fontSize = 12.sp,
                                color = C.textSecondary
                            )
                        }
                        IconButton(onClick = { viewModel.toggleTheme() }) {
                            Icon(
                                if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "Toggle theme",
                                tint = if (isDark) AccentYellow else PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = C.border, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(8.dp))

                    drawerItems.forEach { screen ->
                        val isDrawerSelected = currentRoute == screen.route
                        val unlocked = isScreenUnlocked(screen.route)
                        NavigationDrawerItem(
                            icon = {
                                if (unlocked) {
                                    Icon(
                                        screen.icon,
                                        contentDescription = null,
                                        tint = if (isDrawerSelected) PrimaryBlue else C.textMuted
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = C.textMuted.copy(alpha = 0.5f)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    screen.title,
                                    color = if (!unlocked) C.textMuted.copy(alpha = 0.5f)
                                           else if (isDrawerSelected) PrimaryBlue
                                           else C.textSecondary
                                )
                            },
                            badge = {
                                if (!unlocked) {
                                    Icon(Icons.Filled.Lock, contentDescription = null, tint = C.textMuted.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                                }
                            },
                            selected = isDrawerSelected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                if (!unlocked) {
                                    navController.navigate(Screen.Pricing.route) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                    }
                                } else if (!isDrawerSelected) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = PrimaryBlue.copy(alpha = 0.12f),
                                unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                selectedTextColor = PrimaryBlue,
                                unselectedTextColor = C.textSecondary,
                                selectedIconColor = PrimaryBlue,
                                unselectedIconColor = C.textMuted
                            )
                        )
                    }
                }
            }
        ) {
            Scaffold(
                containerColor = C.background,
                topBar = {
                    if (currentRoute != Screen.Settings.route) {
                        Column {
                            TopAppBar(
                                title = {
                                    Text(
                                        drawerItems.find { it.route == currentRoute }?.title ?: "Custom Dialer",
                                        color = C.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 18.sp
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = C.textPrimary)
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { viewModel.toggleTheme() }) {
                                        Icon(
                                            if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                            contentDescription = "Toggle theme",
                                            tint = if (isDark) AccentYellow else PrimaryBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = C.surface
                                )
                            )
                            // Subtle divider under top bar
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(C.border)
                            )
                        }
                    }
                },
                bottomBar = {
                    if (currentRoute != Screen.Settings.route) {
                        // Custom bottom nav — no Material3 indicator bugs
                        Surface(
                            color = C.surface,
                            shadowElevation = if (isDark) 0.dp else 4.dp,
                            tonalElevation = 0.dp
                        ) {
                            Column {
                                // Top divider line
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(C.border)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp, bottom = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    bottomNavItems.forEach { screen ->
                                        val isSelected = currentRoute == screen.route
                                        val unlocked = isScreenUnlocked(screen.route)
                                        val itemColor = if (!unlocked) C.textMuted.copy(alpha = 0.4f)
                                                        else if (isSelected) PrimaryBlue
                                                        else C.textMuted

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    if (!unlocked) {
                                                        navController.navigate(Screen.Pricing.route) {
                                                            popUpTo(Screen.Dashboard.route) { saveState = true }
                                                            launchSingleTop = true
                                                        }
                                                    } else if (!isSelected) {
                                                        navController.navigate(screen.route) {
                                                            popUpTo(Screen.Dashboard.route) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                }
                                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(48.dp)
                                                    .height(28.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(
                                                        if (isSelected && unlocked) PrimaryBlue.copy(alpha = if (isDark) 0.18f else 0.12f)
                                                        else Color.Transparent
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (unlocked) {
                                                    Icon(screen.icon, contentDescription = screen.title, tint = itemColor, modifier = Modifier.size(22.dp))
                                                } else {
                                                    Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = itemColor, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                screen.title,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected && unlocked) FontWeight.SemiBold else FontWeight.Normal,
                                                color = itemColor,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Dashboard.route) {
                        val loading by viewModel.dashLoading.collectAsStateWithLifecycle()
                        val callingStatusVal by viewModel.callingStatus.collectAsStateWithLifecycle()
                        val custCalls by viewModel.customerCalls.collectAsStateWithLifecycle()

                        LaunchedEffect(Unit) {
                            viewModel.loadCallingStatus()
                            viewModel.loadCustomerCalls()
                            viewModel.loadMyAccount()
                        }

                        DashboardScreen(
                            customerName = viewModel.tokenManager.getCustomerName()
                                ?: agent?.displayName ?: agent?.username,
                            callingStatus = callingStatusVal,
                            recentCalls = custCalls,
                            onRefresh = {
                                viewModel.loadCallingStatus()
                                viewModel.loadCustomerCalls()
                                viewModel.loadMyAccount()
                            },
                            isLoading = loading,
                            onNavigateToStore = {
                                val dest = if (custPackage == "free") Screen.Pricing.route else Screen.Store.route
                                navController.navigate(dest) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(Screen.Dialer.route) {
                        val callingStatusVal by viewModel.callingStatus.collectAsStateWithLifecycle()
                        val myNums by viewModel.myNumbers.collectAsStateWithLifecycle()

                        LaunchedEffect(Unit) {
                            viewModel.loadCallingStatus()
                            viewModel.loadMyAccount()
                        }

                        DialerScreen(
                            onDial = { number ->
                                val cs = callingStatusVal
                                if (cs?.canMakeCalls != true) {
                                    // Don't dial — no number or no minutes
                                } else {
                                    dialVoip(number)
                                }
                            },
                            isOnCall = isOnCall,
                            activeCallNumber = activeCallNumber,
                            callStatus = callStatus,
                            isMuted = isMuted,
                            voiceReady = voiceReady || (callingStatusVal?.canMakeCalls == true),
                            onHangUp = { viewModel.hangup() },
                            onToggleMute = { viewModel.toggleMute() },
                            onSendDtmf = { viewModel.sendDtmf(it) },
                            myNumbers = myNums,
                            activeNumber = callingStatusVal?.phoneNumber,
                            minutesRemaining = callingStatusVal?.minutesRemaining ?: 0,
                            onSelectNumber = { viewModel.selectNumber(it) }
                        )
                    }

                    composable(Screen.CallHistory.route) {
                        val calls by viewModel.callLogs.collectAsStateWithLifecycle()
                        val loading by viewModel.callsLoading.collectAsStateWithLifecycle()
                        val search by viewModel.callSearchQuery.collectAsStateWithLifecycle()
                        val filter by viewModel.callFilter.collectAsStateWithLifecycle()

                        LaunchedEffect(Unit) { viewModel.loadCallLogs(reset = true) }

                        CallHistoryScreen(
                            calls = calls,
                            isLoading = loading,
                            searchQuery = search,
                            onSearchChange = { viewModel.setCallSearch(it) },
                            onLoadMore = { viewModel.loadCallLogs() },
                            onCallClick = { },
                            onDialNumber = { dialVoip(it) },
                            selectedFilter = filter,
                            onFilterChange = { viewModel.setCallFilter(it) }
                        )
                    }

                    composable(Screen.Contacts.route) {
                        val contactList by viewModel.contacts.collectAsStateWithLifecycle()
                        val loading by viewModel.contactsLoading.collectAsStateWithLifecycle()
                        val search by viewModel.contactSearchQuery.collectAsStateWithLifecycle()

                        LaunchedEffect(Unit) { viewModel.loadContacts(reset = true) }

                        ContactsScreen(
                            contacts = contactList,
                            isLoading = loading,
                            searchQuery = search,
                            onSearchChange = { viewModel.setContactSearch(it) },
                            onLoadMore = { viewModel.loadContacts() },
                            onContactClick = { },
                            onDialContact = { dialVoip(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onDeleteContact = { viewModel.deleteContact(it) },
                            onCreateContact = { viewModel.createContact(it) }
                        )
                    }

                    composable(Screen.Store.route) {
                        val numbers by viewModel.availableNumbers.collectAsStateWithLifecycle()
                        val numbersLoading by viewModel.numbersLoading.collectAsStateWithLifecycle()
                        val numberType by viewModel.selectedNumberType.collectAsStateWithLifecycle()
                        val packages by viewModel.minutesPackages.collectAsStateWithLifecycle()
                        val packagesLoading by viewModel.packagesLoading.collectAsStateWithLifecycle()
                        val purchaseMsg by viewModel.purchaseMessage.collectAsStateWithLifecycle()
                        LaunchedEffect(Unit) {
                            viewModel.loadAvailableNumbers()
                            viewModel.loadMinutesPackages()
                        }

                        StoreScreen(
                            availableNumbers = numbers,
                            numbersLoading = numbersLoading,
                            selectedNumberType = numberType,
                            onNumberTypeChange = { viewModel.loadAvailableNumbers(it) },
                            onRefreshNumbers = { viewModel.refreshNumbers() },
                            minutesPackages = packages,
                            packagesLoading = packagesLoading,
                            onBuyPackage = { viewModel.purchaseMinutes(it) },
                            onBuyNumber = { viewModel.purchaseNumber(it) },
                            purchaseMessage = purchaseMsg
                        )
                    }

                    composable(Screen.PowerDialer.route) {
                        val lists by viewModel.phoneLists.collectAsStateWithLifecycle()
                        val selectedList by viewModel.selectedPhoneList.collectAsStateWithLifecycle()
                        val entries by viewModel.phoneListEntries.collectAsStateWithLifecycle()
                        val progress by viewModel.powerDialProgress.collectAsStateWithLifecycle()
                        val loading by viewModel.listsLoading.collectAsStateWithLifecycle()

                        LaunchedEffect(Unit) { viewModel.loadPhoneLists() }

                        PowerDialerScreen(
                            phoneLists = lists,
                            selectedList = selectedList,
                            entries = entries,
                            progress = progress,
                            isLoading = loading,
                            onSelectList = { viewModel.selectPhoneList(it) },
                            onBackToLists = { viewModel.selectPhoneList(null) },
                            onDialEntry = { entry -> entry.phoneNumber?.let { dialVoip(it) } },
                            onUpdateStatus = { id, status -> viewModel.updateEntryStatus(id, status) },
                            onDeleteList = { viewModel.deletePhoneList(it) },
                            onAutoDialNext = { }
                        )
                    }

                    composable(Screen.Email.route) {
                        val emailList by viewModel.emails.collectAsStateWithLifecycle()
                        val unread by viewModel.unreadCount.collectAsStateWithLifecycle()
                        val loading by viewModel.emailsLoading.collectAsStateWithLifecycle()
                        val folder by viewModel.emailFolder.collectAsStateWithLifecycle()

                        LaunchedEffect(Unit) { viewModel.loadEmails() }

                        EmailScreen(
                            emails = emailList,
                            unreadCount = unread,
                            isLoading = loading,
                            selectedFolder = folder,
                            onFolderChange = { viewModel.loadEmails(it) },
                            onEmailClick = { },
                            onSync = { viewModel.syncEmails() },
                            onRefresh = { viewModel.loadEmails(folder) }
                        )
                    }

                    composable(Screen.Profile.route) {
                        val att by viewModel.attendance.collectAsStateWithLifecycle()
                        val myMin by viewModel.myMinutes.collectAsStateWithLifecycle()
                        val myNums by viewModel.myNumbers.collectAsStateWithLifecycle()
                        val purchases by viewModel.purchaseHistory.collectAsStateWithLifecycle()
                        val payMethod by viewModel.paymentMethod.collectAsStateWithLifecycle()
                        LaunchedEffect(Unit) {
                            viewModel.loadProfile()
                            viewModel.loadAttendance()
                            viewModel.loadMyAccount()
                            viewModel.loadPaymentMethod()
                            viewModel.loadCallingStatus()
                        }

                        ProfileScreen(
                            agent = agent,
                            attendance = att,
                            onClockIn = { viewModel.clockIn() },
                            onClockOut = { viewModel.clockOut() },
                            onLogout = { viewModel.logout() },
                            onNavigateToSettings = {
                                navController.navigate(Screen.Settings.route)
                            },
                            customerName = viewModel.tokenManager.getCustomerName(),
                            customerEmail = viewModel.tokenManager.getCustomerEmail(),
                            myMinutes = myMin,
                            myNumbers = myNums,
                            purchaseHistory = purchases,
                            paymentMethod = payMethod,
                            onAddCard = { viewModel.setupCard() }
                        )
                    }

                    composable(Screen.Pricing.route) {
                        val pkgs by viewModel.subPackages.collectAsStateWithLifecycle()
                        val msg by viewModel.purchaseMessage.collectAsStateWithLifecycle()

                        LaunchedEffect(Unit) { viewModel.loadSubPackages() }

                        PricingScreen(
                            packages = pkgs,
                            currentPackage = custPackage,
                            onBuyPackage = { viewModel.buyPackage(it) },
                            purchaseMessage = msg,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.PurchaseSuccess.route) {
                        PurchaseSuccessScreen(
                            onGoToDashboard = {
                                viewModel.dismissPurchaseSuccess()
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onGoToStore = {
                                viewModel.dismissPurchaseSuccess()
                                navController.navigate(Screen.Store.route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            currentBaseUrl = RetrofitClient.BASE_URL,
                            onSaveBaseUrl = { url ->
                                viewModel.tokenManager.saveBaseUrl(url)
                                RetrofitClient.updateBaseUrl(url)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }

        // Incoming call overlay - shown on top of everything
        if (hasIncomingCall && incomingCallFrom != null) {
            IncomingCallOverlay(
                fromNumber = incomingCallFrom ?: "Unknown",
                callerName = incomingCallerName,
                onAccept = { viewModel.acceptIncomingCall() },
                onReject = { viewModel.rejectIncomingCall() }
            )
        }
    }
}
