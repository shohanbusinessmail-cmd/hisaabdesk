package com.shohankhan.hisaabdesk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shohankhan.hisaabdesk.feature.accounts.AccountDetailScreen
import com.shohankhan.hisaabdesk.feature.accounts.AccountFormScreen
import com.shohankhan.hisaabdesk.feature.accounts.AccountListScreen
import com.shohankhan.hisaabdesk.feature.accounts.TransferScreen
import com.shohankhan.hisaabdesk.feature.authentication.PinChangeScreen
import com.shohankhan.hisaabdesk.feature.authentication.PinCreateScreen
import com.shohankhan.hisaabdesk.feature.authentication.PinLockScreen
import com.shohankhan.hisaabdesk.feature.authentication.SecurityViewModel
import com.shohankhan.hisaabdesk.feature.backup.BackupScreen
import com.shohankhan.hisaabdesk.feature.categories.CategoryFormScreen
import com.shohankhan.hisaabdesk.feature.categories.CategoryListScreen
import com.shohankhan.hisaabdesk.feature.dashboard.DashboardScreen
import com.shohankhan.hisaabdesk.feature.debts.BorrowingDetailScreen
import com.shohankhan.hisaabdesk.feature.debts.BorrowingFormScreen
import com.shohankhan.hisaabdesk.feature.debts.ContactDetailScreen
import com.shohankhan.hisaabdesk.feature.debts.ContactFormScreen
import com.shohankhan.hisaabdesk.feature.debts.ContactListScreen
import com.shohankhan.hisaabdesk.feature.debts.DebtListScreen
import com.shohankhan.hisaabdesk.feature.debts.LendingDetailScreen
import com.shohankhan.hisaabdesk.feature.debts.LendingFormScreen
import com.shohankhan.hisaabdesk.feature.debts.ObligationPaymentScreen
import com.shohankhan.hisaabdesk.feature.debts.PayableDetailScreen
import com.shohankhan.hisaabdesk.feature.debts.PayableFormScreen
import com.shohankhan.hisaabdesk.feature.debts.PayableListScreen
import com.shohankhan.hisaabdesk.feature.debts.ReceivableDetailScreen
import com.shohankhan.hisaabdesk.feature.debts.ReceivableFormScreen
import com.shohankhan.hisaabdesk.feature.debts.ReceivableListScreen
import com.shohankhan.hisaabdesk.feature.home.MoreScreen
import com.shohankhan.hisaabdesk.feature.loans.LoanDetailScreen
import com.shohankhan.hisaabdesk.feature.loans.LoanFormScreen
import com.shohankhan.hisaabdesk.feature.loans.LoanListScreen
import com.shohankhan.hisaabdesk.feature.loans.LoanPaymentScreen
import com.shohankhan.hisaabdesk.feature.onboarding.OnboardingAccountScreen
import com.shohankhan.hisaabdesk.feature.onboarding.OnboardingCategoriesScreen
import com.shohankhan.hisaabdesk.feature.onboarding.OnboardingDoneScreen
import com.shohankhan.hisaabdesk.feature.onboarding.OnboardingPrivacyScreen
import com.shohankhan.hisaabdesk.feature.onboarding.OnboardingWelcomeScreen
import com.shohankhan.hisaabdesk.feature.planning.BillFormScreen
import com.shohankhan.hisaabdesk.feature.planning.BillListScreen
import com.shohankhan.hisaabdesk.feature.planning.BudgetDetailScreen
import com.shohankhan.hisaabdesk.feature.planning.BudgetFormScreen
import com.shohankhan.hisaabdesk.feature.planning.BudgetListScreen
import com.shohankhan.hisaabdesk.feature.planning.PaymentCalendarScreen
import com.shohankhan.hisaabdesk.feature.reports.ReportScreen
import com.shohankhan.hisaabdesk.feature.savings.GoalDetailScreen
import com.shohankhan.hisaabdesk.feature.savings.GoalFormScreen
import com.shohankhan.hisaabdesk.feature.savings.SavingsDetailScreen
import com.shohankhan.hisaabdesk.feature.savings.SavingsFormScreen
import com.shohankhan.hisaabdesk.feature.savings.SavingsListScreen
import com.shohankhan.hisaabdesk.feature.savings.SavingsTransactionScreen
import com.shohankhan.hisaabdesk.feature.settings.AboutScreen
import com.shohankhan.hisaabdesk.feature.settings.OnboardingFlags
import com.shohankhan.hisaabdesk.feature.settings.ReconcileScreen
import com.shohankhan.hisaabdesk.feature.settings.SecuritySettingsScreen
import com.shohankhan.hisaabdesk.feature.settings.SettingsScreen
import com.shohankhan.hisaabdesk.feature.settings.TrashScreen
import com.shohankhan.hisaabdesk.feature.settings.SettingsViewModel
import com.shohankhan.hisaabdesk.feature.shopping.PurchaseDetailScreen
import com.shohankhan.hisaabdesk.feature.shopping.PurchaseFormScreen
import com.shohankhan.hisaabdesk.feature.shopping.PurchasePaymentScreen
import com.shohankhan.hisaabdesk.feature.shopping.ShopDetailScreen
import com.shohankhan.hisaabdesk.feature.shopping.ShopListScreen
import com.shohankhan.hisaabdesk.feature.shopping.ShoppingListScreen
import com.shohankhan.hisaabdesk.feature.transactions.FormMode
import com.shohankhan.hisaabdesk.feature.transactions.TransactionDetailScreen
import com.shohankhan.hisaabdesk.feature.transactions.TransactionFormScreen
import com.shohankhan.hisaabdesk.feature.transactions.TransactionListScreen
import com.shohankhan.hisaabdesk.ui.appViewModel
import com.shohankhan.hisaabdesk.ui.navigation.Routes
import com.shohankhan.hisaabdesk.ui.theme.HisaabDeskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HisaabDeskTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as HisaabDeskApplication
    val flags = remember { OnboardingFlags(context) }
    val securityVm: SecurityViewModel = appViewModel { SecurityViewModel(it.container) }
    val security by securityVm.ui.collectAsState()

    var onboardingComplete by remember { mutableStateOf(flags.onboardingComplete) }

    // Auto-lock on background/inactivity, per user setting.
    val settingsVm: SettingsViewModel = appViewModel { SettingsViewModel(it.container) }
    val userSettings by settingsVm.settings.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var stoppedAt by remember { mutableStateOf<Long?>(null) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> stoppedAt = System.currentTimeMillis()
                Lifecycle.Event.ON_START -> {
                    val timeoutMinutes = userSettings.lockTimeoutMinutes
                    val last = stoppedAt
                    if (security.pinSet && timeoutMinutes >= 0 && last != null) {
                        val elapsed = System.currentTimeMillis() - last
                        if (timeoutMinutes == 0 || elapsed > timeoutMinutes * 60_000L) {
                            securityVm.lockNow()
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when {
        !onboardingComplete -> OnboardingFlow(
            onFinished = {
                flags.onboardingComplete = true
                onboardingComplete = true
            }
        )
        security.locked -> PinLockScreen(onUnlocked = { })
        else -> MainNavHost(onLockNow = { securityVm.lockNow() })
    }
}

@Composable
private fun OnboardingFlow(onFinished: () -> Unit) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.ONBOARDING_WELCOME) {
        composable(Routes.ONBOARDING_WELCOME) {
            OnboardingWelcomeScreen(onNext = { nav.navigate(Routes.ONBOARDING_PRIVACY) })
        }
        composable(Routes.ONBOARDING_PRIVACY) {
            OnboardingPrivacyScreen(
                onNext = { nav.navigate(Routes.ONBOARDING_PIN) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.ONBOARDING_PIN) {
            PinCreateScreen(
                title = "পিন তৈরি করুন",
                body = "৪ থেকে ৬ সংখ্যার একটি পিন দিন। এই পিন দিয়েই প্রতিবার হিসাবডেস্ক খুলবেন।",
                onBack = { nav.popBackStack() },
                onDone = { nav.navigate(Routes.ONBOARDING_ACCOUNT) }
            )
        }
        composable(Routes.ONBOARDING_ACCOUNT) {
            OnboardingAccountScreen(
                onNext = { nav.navigate(Routes.ONBOARDING_CATEGORIES) },
                onSkip = { nav.navigate(Routes.ONBOARDING_CATEGORIES) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.ONBOARDING_CATEGORIES) {
            OnboardingCategoriesScreen(
                onNext = { nav.navigate(Routes.ONBOARDING_DONE) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.ONBOARDING_DONE) {
            OnboardingDoneScreen(onFinish = onFinished)
        }
    }
}

@Composable
private fun MainNavHost(onLockNow: () -> Unit) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val topLevel = listOf(Routes.DASHBOARD, Routes.TRANSACTIONS, Routes.ACCOUNTS, "more")
    val showBottomBar = currentRoute in topLevel

    fun go(route: String) {
        nav.navigate(route) { launchSingleTop = true }
    }

    fun goTop(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = currentRoute == Routes.DASHBOARD,
                        onClick = { goTop(Routes.DASHBOARD) },
                        icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) },
                        label = { Text("ড্যাশবোর্ড") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.TRANSACTIONS,
                        onClick = { goTop(Routes.TRANSACTIONS) },
                        icon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
                        label = { Text("লেনদেন") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.ACCOUNTS,
                        onClick = { goTop(Routes.ACCOUNTS) },
                        icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
                        label = { Text("অ্যাকাউন্ট") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "more",
                        onClick = { goTop("more") },
                        icon = { Icon(Icons.Filled.MoreHoriz, contentDescription = null) },
                        label = { Text("আরও") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            // ---------- Dashboard ----------
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onOpenTransactions = { goTop(Routes.TRANSACTIONS) },
                    onOpenTransaction = { id -> go(Routes.txnDetail(id)) },
                    onOpenAccounts = { goTop(Routes.ACCOUNTS) },
                    onQuickAddIncome = { go(Routes.txnForm("INCOME")) },
                    onQuickAddExpense = { go(Routes.txnForm("EXPENSE")) },
                    onOpenSavings = { go(Routes.SAVINGS) },
                    onOpenReceivables = { go(Routes.RECEIVABLES) },
                    onOpenPayables = { go(Routes.PAYABLES) },
                    onOpenShopping = { go(Routes.SHOPPING) },
                    onOpenDebts = { go(Routes.DEBTS) },
                    onOpenLoans = { kind -> go(Routes.loans(kind)) }
                )
            }
            composable("more") {
                MoreScreen(
                    onShopping = { go(Routes.SHOPPING) },
                    onContacts = { go(Routes.CONTACTS) },
                    onDebts = { go(Routes.DEBTS) },
                    onReceivables = { go(Routes.RECEIVABLES) },
                    onPayables = { go(Routes.PAYABLES) },
                    onNgoLoans = { go(Routes.loans("NGO")) },
                    onBankLoans = { go(Routes.loans("BANK")) },
                    onSavings = { go(Routes.SAVINGS) },
                    onBudgets = { go(Routes.BUDGETS) },
                    onGoals = { go(Routes.GOALS) },
                    onBills = { go(Routes.BILLS) },
                    onCalendar = { go(Routes.CALENDAR) },
                    onReports = { go(Routes.REPORTS) },
                    onCategories = { go(Routes.CATEGORIES) },
                    onBackup = { go(Routes.BACKUP) },
                    onSettings = { go(Routes.SETTINGS) },
                    onAbout = { go(Routes.ABOUT) }
                )
            }

            // ---------- Transactions ----------
            composable(Routes.TRANSACTIONS) {
                TransactionListScreen(
                    onBack = { goTop(Routes.DASHBOARD) },
                    onOpenTxn = { id -> go(Routes.txnDetail(id)) },
                    onAddIncome = { go(Routes.txnForm("INCOME")) },
                    onAddExpense = { go(Routes.txnForm("EXPENSE")) }
                )
            }
            composable(
                route = Routes.TXN_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                TransactionDetailScreen(
                    id = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { go(Routes.txnForm("EDIT", it)) }
                )
            }
            composable(
                route = Routes.TXN_FORM,
                arguments = listOf(
                    navArgument("mode") { type = NavType.StringType; defaultValue = "INCOME" },
                    navArgument("id") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { entry ->
                val modeArg = entry.arguments?.getString("mode") ?: "INCOME"
                val id = entry.arguments?.getLong("id") ?: 0L
                val mode = when (modeArg) {
                    "EXPENSE" -> FormMode.Expense
                    "TRANSFER" -> FormMode.Transfer
                    "EDIT" -> FormMode.Edit(id)
                    else -> FormMode.Income
                }
                TransactionFormScreen(mode = mode, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }

            // ---------- Accounts ----------
            composable(Routes.ACCOUNTS) {
                AccountListScreen(
                    onBack = { goTop(Routes.DASHBOARD) },
                    onAdd = { go(Routes.accountForm()) },
                    onOpen = { go(Routes.accountDetail(it)) }
                )
            }
            composable(
                route = Routes.ACCOUNT_FORM,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                AccountFormScreen(id = id, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(
                route = Routes.ACCOUNT_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                AccountDetailScreen(id = id, onBack = { nav.popBackStack() }, onEdit = { go(Routes.accountForm(id)) })
            }
            composable(Routes.TRANSFER) {
                TransferScreen(onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() })
            }

            // ---------- Categories ----------
            composable(Routes.CATEGORIES) {
                CategoryListScreen(
                    onBack = { nav.popBackStack() },
                    onAdd = { go(Routes.categoryForm()) },
                    onEdit = { go(Routes.categoryForm(it)) }
                )
            }
            composable(
                route = Routes.CATEGORY_FORM,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                CategoryFormScreen(id = id, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }

            // ---------- Shopping ----------
            composable(Routes.SHOPPING) {
                ShoppingListScreen(
                    onBack = { nav.popBackStack() },
                    onNewPurchase = { go(Routes.purchaseForm()) },
                    onOpenPurchase = { go(Routes.purchaseDetail(it)) },
                    onOpenShops = { go(Routes.SHOPS) }
                )
            }
            composable(
                route = Routes.PURCHASE_FORM,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                PurchaseFormScreen(existingId = id, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(
                route = Routes.PURCHASE_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                PurchaseDetailScreen(
                    purchaseId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { go(Routes.purchaseForm(id)) },
                    onAddPayment = { go(Routes.purchasePayment(id)) }
                )
            }
            composable(
                route = Routes.PURCHASE_PAYMENT,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                PurchasePaymentScreen(purchaseId = id, onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() })
            }
            composable(Routes.SHOPS) {
                ShopListScreen(onBack = { nav.popBackStack() }, onOpenShop = { go(Routes.shopDetail(it)) })
            }
            composable(
                route = Routes.SHOP_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                ShopDetailScreen(shopId = id, onBack = { nav.popBackStack() }, onOpenPurchase = { go(Routes.purchaseDetail(it)) })
            }

            // ---------- Contacts & debts ----------
            composable(Routes.CONTACTS) {
                ContactListScreen(
                    onBack = { nav.popBackStack() },
                    onAdd = { go(Routes.contactForm()) },
                    onOpen = { go(Routes.contactDetail(it)) }
                )
            }
            composable(
                route = Routes.CONTACT_FORM,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                ContactFormScreen(id = id, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(
                route = Routes.CONTACT_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                ContactDetailScreen(contactId = id, onBack = { nav.popBackStack() }, onEdit = { go(Routes.contactForm(id)) })
            }
            composable(Routes.DEBTS) {
                DebtListScreen(
                    onBack = { nav.popBackStack() },
                    onAddLending = { go(Routes.lendingForm()) },
                    onAddBorrowing = { go(Routes.borrowingForm()) },
                    onOpenLending = { go(Routes.lendingDetail(it)) },
                    onOpenBorrowing = { go(Routes.borrowingDetail(it)) }
                )
            }
            composable(
                route = Routes.LENDING_FORM,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                LendingFormScreen(id = id, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(
                route = Routes.LENDING_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                LendingDetailScreen(
                    id = id,
                    onBack = { nav.popBackStack() },
                    onAddPayment = { go(Routes.obligationPayment("LENDING", id)) }
                )
            }
            composable(
                route = Routes.BORROWING_FORM,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                BorrowingFormScreen(id = id, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(
                route = Routes.BORROWING_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                BorrowingDetailScreen(
                    id = id,
                    onBack = { nav.popBackStack() },
                    onAddPayment = { go(Routes.obligationPayment("BORROWING", id)) }
                )
            }
            composable(
                route = Routes.OBLIGATION_PAYMENT,
                arguments = listOf(
                    navArgument("kind") { type = NavType.StringType },
                    navArgument("id") { type = NavType.LongType }
                )
            ) { entry ->
                val kind = entry.arguments?.getString("kind") ?: "LENDING"
                val id = entry.arguments?.getLong("id") ?: 0L
                ObligationPaymentScreen(kind = kind, id = id, onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() })
            }

            // ---------- Receivables / payables ----------
            composable(Routes.RECEIVABLES) {
                ReceivableListScreen(
                    onBack = { nav.popBackStack() },
                    onAdd = { go(Routes.receivableForm()) },
                    onOpen = { go(Routes.receivableDetail(it)) }
                )
            }
            composable(Routes.PAYABLES) {
                PayableListScreen(
                    onBack = { nav.popBackStack() },
                    onAdd = { go(Routes.payableForm()) },
                    onOpen = { go(Routes.payableDetail(it)) }
                )
            }
            composable(
                route = Routes.RECEIVABLE_FORM,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                ReceivableFormScreen(id = id, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(
                route = Routes.PAYABLE_FORM,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                PayableFormScreen(id = id, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(
                route = Routes.RECEIVABLE_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                ReceivableDetailScreen(
                    id = id,
                    onBack = { nav.popBackStack() },
                    onAddPayment = { go(Routes.obligationPayment("RECEIVABLE", id)) }
                )
            }
            composable(
                route = Routes.PAYABLE_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                PayableDetailScreen(
                    id = id,
                    onBack = { nav.popBackStack() },
                    onAddPayment = { go(Routes.obligationPayment("PAYABLE", id)) }
                )
            }

            // ---------- Loans ----------
            composable(
                route = Routes.LOANS,
                arguments = listOf(navArgument("kind") { type = NavType.StringType })
            ) { entry ->
                val kind = entry.arguments?.getString("kind") ?: "NGO"
                LoanListScreen(
                    kind = kind,
                    onBack = { nav.popBackStack() },
                    onAdd = { go(Routes.loanForm(kind)) },
                    onOpen = { go(Routes.loanDetail(it)) }
                )
            }
            composable(
                route = Routes.LOAN_FORM,
                arguments = listOf(
                    navArgument("kind") { type = NavType.StringType },
                    navArgument("id") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { entry ->
                val kind = entry.arguments?.getString("kind") ?: "NGO"
                val id = entry.arguments?.getLong("id") ?: 0L
                LoanFormScreen(kind = kind, existingId = id, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(
                route = Routes.LOAN_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                LoanDetailScreen(loanId = id, onBack = { nav.popBackStack() }, onAddPayment = { go(Routes.loanPayment(id)) })
            }
            composable(
                route = Routes.LOAN_PAYMENT,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                LoanPaymentScreen(loanId = id, onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() })
            }

            // ---------- Savings & goals ----------
            composable(Routes.SAVINGS) {
                SavingsListScreen(
                    onBack = { nav.popBackStack() },
                    onAdd = { go(Routes.savingsForm()) },
                    onOpen = { go(Routes.savingsDetail(it)) },
                    onOpenGoal = { go(Routes.goalDetail(it)) },
                    onAddGoal = { go(Routes.goalForm()) }
                )
            }
            composable(
                route = Routes.SAVINGS_FORM,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                SavingsFormScreen(id = id, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(
                route = Routes.SAVINGS_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                SavingsDetailScreen(id = id, onBack = { nav.popBackStack() }, onTransaction = { go(Routes.savingsTxn(id)) })
            }
            composable(
                route = Routes.SAVINGS_TXN,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                SavingsTransactionScreen(accountId = id, onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() })
            }
            composable(Routes.GOALS) {
                GoalsGate(
                    onBack = { nav.popBackStack() },
                    onAdd = { go(Routes.goalForm()) },
                    onOpen = { go(Routes.goalDetail(it)) }
                )
            }
            composable(
                route = Routes.GOAL_FORM,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                GoalFormScreen(id = id, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(
                route = Routes.GOAL_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                GoalDetailScreen(id = id, onBack = { nav.popBackStack() }, onEdit = { go(Routes.goalForm(id)) })
            }

            // ---------- Budgets, bills, calendar ----------
            composable(Routes.BUDGETS) {
                BudgetListScreen(
                    onBack = { nav.popBackStack() },
                    onAdd = { go(Routes.budgetForm()) },
                    onOpen = { go("budget/$it") }
                )
            }
            composable(
                route = Routes.BUDGET_FORM,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                BudgetFormScreen(id = id, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(
                route = "budget/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                BudgetDetailScreen(id = id, onBack = { nav.popBackStack() }, onEdit = { go(Routes.budgetForm(id)) })
            }
            composable(Routes.BILLS) {
                BillListScreen(
                    onBack = { nav.popBackStack() },
                    onAdd = { go(Routes.billForm()) },
                    onEdit = { go(Routes.billForm(it)) },
                    onCalendar = { go(Routes.CALENDAR) }
                )
            }
            composable(
                route = Routes.BILL_FORM,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                BillFormScreen(id = id, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }
            composable(Routes.CALENDAR) {
                PaymentCalendarScreen(onBack = { nav.popBackStack() }, onOpenBills = { go(Routes.BILLS) })
            }

            // ---------- Reports ----------
            composable(Routes.REPORTS) {
                ReportScreen(onBack = { nav.popBackStack() })
            }

            // ---------- Settings & misc ----------
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenSecurity = { go(Routes.SECURITY) },
                    onOpenCategories = { go(Routes.CATEGORIES) },
                    onOpenBackup = { go(Routes.BACKUP) },
                    onOpenAbout = { go(Routes.ABOUT) },
                    onOpenTrash = { go(Routes.TRASH) },
                    onOpenReconcile = { go(Routes.RECONCILE) }
                )
            }
            composable(Routes.SECURITY) {
                SecuritySettingsScreen(onBack = { nav.popBackStack() }, onChangePin = { go("pin/change") })
            }
            composable("pin/change") {
                PinChangeScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.BACKUP) {
                BackupScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.TRASH) {
                TrashScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.RECONCILE) {
                ReconcileScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}

/** Goals list lives in the savings module; this keeps a stable dedicated route. */
@Composable
private fun GoalsGate(onBack: () -> Unit, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    com.shohankhan.hisaabdesk.feature.savings.GoalListScreen(
        onBack = onBack,
        onAdd = onAdd,
        onOpen = onOpen
    )
}
