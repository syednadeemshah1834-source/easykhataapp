package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.model.PartyType
import com.example.data.repository.LedgerRepository
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.BusinessesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PartiesScreen
import com.example.ui.screens.PartyLedgerScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.DuesLedgerTheme
import com.example.ui.theme.Navy800
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.LedgerViewModel
import com.example.ui.viewmodel.LedgerViewModelFactory

enum class NavigationDestination {
    DASHBOARD,
    CUSTOMERS,
    SUPPLIERS,
    REPORTS,
    MORE,
    BUSINESS_MANAGER,
    BACKUP_RESTORE,
    PARTY_LEDGER
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = LedgerRepository(database)
        val viewModelFactory = LedgerViewModelFactory(repository)

        setContent {
            var isDarkMode by remember { mutableStateOf(false) }

            DuesLedgerTheme(darkTheme = isDarkMode) {
                val viewModel: LedgerViewModel = viewModel(factory = viewModelFactory)
                MainAppScreen(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: LedgerViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val allBusinesses by viewModel.businesses.collectAsState()
    val activeBizId by viewModel.selectedBusinessId.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val allTx by viewModel.allBusinessTransactions.collectAsState()
    val activePartyWithBalance by viewModel.activePartyWithBalance.collectAsState()
    val activeLedger by viewModel.activePartyLedger.collectAsState()
    val reportsDateFilter by viewModel.reportsDateFilter.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var currentDestination by remember { mutableStateOf(NavigationDestination.DASHBOARD) }
    var previousDestination by remember { mutableStateOf(NavigationDestination.DASHBOARD) }

    val currency = currentBusiness?.currencySymbol?.takeIf { it != "$" } ?: "Rs"

    // Observe snackbar notifications
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (currentDestination != NavigationDestination.PARTY_LEDGER) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "DuesLedger",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            )
                            if (currentBusiness != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "• ${currentBusiness?.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onToggleDarkMode,
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Dark Mode"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (currentDestination != NavigationDestination.PARTY_LEDGER) {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars,
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    val items = listOf(
                        Triple(NavigationDestination.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
                        Triple(NavigationDestination.CUSTOMERS, "Customers", Icons.Default.Groups),
                        Triple(NavigationDestination.SUPPLIERS, "Suppliers", Icons.Default.Inventory),
                        Triple(NavigationDestination.REPORTS, "Reports", Icons.Default.Assessment),
                        Triple(NavigationDestination.MORE, "More", Icons.Default.MoreHoriz)
                    )

                    items.forEach { (dest, label, icon) ->
                        val selected = when (dest) {
                            NavigationDestination.MORE -> currentDestination == NavigationDestination.MORE ||
                                currentDestination == NavigationDestination.BUSINESS_MANAGER ||
                                currentDestination == NavigationDestination.BACKUP_RESTORE
                            else -> currentDestination == dest
                        }

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                previousDestination = currentDestination
                                currentDestination = dest
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryBlue,
                                selectedTextColor = PrimaryBlue,
                                indicatorColor = PrimaryBlue.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${label.lowercase()}")
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                NavigationDestination.DASHBOARD -> {
                    DashboardScreen(
                        currentBusiness = currentBusiness,
                        stats = stats,
                        onNavigateToCustomers = { currentDestination = NavigationDestination.CUSTOMERS },
                        onNavigateToSuppliers = { currentDestination = NavigationDestination.SUPPLIERS },
                        onNavigateToBusinesses = { currentDestination = NavigationDestination.BUSINESS_MANAGER },
                        onSeedDemoData = { viewModel.seedDemoData() },
                        onReverseTransaction = { id, reason ->
                            viewModel.reverseTransaction(id, reason)
                        }
                    )
                }

                NavigationDestination.CUSTOMERS -> {
                    PartiesScreen(
                        partyType = PartyType.CUSTOMER,
                        parties = customers,
                        currencySymbol = currency,
                        onPartyClick = { id ->
                            viewModel.selectParty(id)
                            previousDestination = NavigationDestination.CUSTOMERS
                            currentDestination = NavigationDestination.PARTY_LEDGER
                        },
                        onAddParty = { name, phone, address, notes, openingBal ->
                            viewModel.addParty(name, phone, address, notes, openingBal, PartyType.CUSTOMER)
                        }
                    )
                }

                NavigationDestination.SUPPLIERS -> {
                    PartiesScreen(
                        partyType = PartyType.SUPPLIER,
                        parties = suppliers,
                        currencySymbol = currency,
                        onPartyClick = { id ->
                            viewModel.selectParty(id)
                            previousDestination = NavigationDestination.SUPPLIERS
                            currentDestination = NavigationDestination.PARTY_LEDGER
                        },
                        onAddParty = { name, phone, address, notes, openingBal ->
                            viewModel.addParty(name, phone, address, notes, openingBal, PartyType.SUPPLIER)
                        }
                    )
                }

                NavigationDestination.REPORTS -> {
                    ReportsScreen(
                        currentBusiness = currentBusiness,
                        allTransactions = allTx,
                        customers = customers,
                        suppliers = suppliers,
                        dateFilter = reportsDateFilter,
                        onSelectDatePreset = { preset -> viewModel.setReportsFilter(preset) }
                    )
                }

                NavigationDestination.MORE -> {
                    MoreToolsScreen(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = onToggleDarkMode,
                        onNavigateToBusinesses = { currentDestination = NavigationDestination.BUSINESS_MANAGER },
                        onNavigateToBackup = { currentDestination = NavigationDestination.BACKUP_RESTORE },
                        onSeedDemoData = { viewModel.seedDemoData() }
                    )
                }

                NavigationDestination.BUSINESS_MANAGER -> {
                    BackHandler { currentDestination = NavigationDestination.MORE }
                    BusinessesScreen(
                        businesses = allBusinesses,
                        activeBusinessId = activeBizId,
                        onSelectBusiness = { id -> viewModel.selectBusiness(id) },
                        onCreateBusiness = { name, type, owner, phone, address, curr ->
                            viewModel.createBusiness(name, type, owner, phone, address, curr)
                        },
                        onUpdateBusiness = { b -> viewModel.updateBusiness(b) },
                        onDeleteBusiness = { id -> viewModel.deleteBusiness(id) }
                    )
                }

                NavigationDestination.BACKUP_RESTORE -> {
                    BackHandler { currentDestination = NavigationDestination.MORE }
                    BackupRestoreScreen(
                        onExportBackupJson = { viewModel.exportBackupJson() },
                        onRestoreBackupJson = { json, overwrite, onResult ->
                            viewModel.restoreBackupFromJson(json, overwrite, onResult)
                        },
                        onSeedDemoData = { viewModel.seedDemoData() }
                    )
                }

                NavigationDestination.PARTY_LEDGER -> {
                    PartyLedgerScreen(
                        partyWithBalance = activePartyWithBalance,
                        ledger = activeLedger,
                        currencySymbol = currency,
                        onBack = {
                            viewModel.selectParty(null)
                            currentDestination = previousDestination
                        },
                        onRecordTransaction = { type, amount, refNo, desc, notes ->
                            val party = activePartyWithBalance?.party ?: return@PartyLedgerScreen
                            viewModel.recordTransaction(
                                partyId = party.id,
                                partyType = PartyType.valueOf(party.type),
                                transactionType = type,
                                amount = amount,
                                date = System.currentTimeMillis(),
                                referenceNo = refNo,
                                description = desc,
                                notes = notes
                            )
                        },
                        onReverseTransaction = { id, reason ->
                            viewModel.reverseTransaction(id, reason)
                        },
                        onUpdateParty = { p -> viewModel.updateParty(p) },
                        onDeleteParty = { id ->
                            viewModel.deleteParty(id)
                            currentDestination = previousDestination
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MoreToolsScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onNavigateToBusinesses: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onSeedDemoData: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Business Tools & Settings",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Multiple Businesses Option
        SettingsMenuItem(
            icon = Icons.Default.Business,
            title = "Manage Multiple Businesses",
            subtitle = "Switch between businesses, create store profiles, and customize currencies",
            onClick = onNavigateToBusinesses
        )

        // Offline Backup & Restore Option
        SettingsMenuItem(
            icon = Icons.Default.Backup,
            title = "Backup & Restore",
            subtitle = "Export private offline backups, transfer data, or restore records",
            onClick = onNavigateToBackup
        )

        // Seed Sample Data Option
        SettingsMenuItem(
            icon = Icons.Default.RestartAlt,
            title = "Load Sample Store Data",
            subtitle = "Seed sample customers, suppliers, credit sales, and reversals for instant testing",
            onClick = onSeedDemoData
        )

        // Theme Toggle Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Dark Theme",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = if (isDarkMode) "Dark mode enabled" else "Light mode enabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onToggleDarkMode() }
                )
            }
        }
    }
}

@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
