package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.PartyEntity
import com.example.data.database.TransactionEntity
import com.example.data.model.LedgerItem
import com.example.data.model.PartyType
import com.example.data.model.PartyWithBalance
import com.example.data.model.TransactionType
import com.example.ui.components.AddEditPartyDialog
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.BalanceBadge
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.ReverseTransactionDialog
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.components.formatShortDate
import com.example.ui.theme.MoneyInGreen
import com.example.ui.theme.MoneyInGreenContainer
import com.example.ui.theme.MoneyOutRed
import com.example.ui.theme.MoneyOutRedContainer
import com.example.ui.theme.Navy800
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyLedgerScreen(
    partyWithBalance: PartyWithBalance?,
    ledger: List<LedgerItem>,
    currencySymbol: String,
    onBack: () -> Unit,
    onRecordTransaction: (
        type: TransactionType,
        amount: Double,
        referenceNo: String,
        description: String,
        notes: String
    ) -> Unit,
    onReverseTransaction: (id: Long, reason: String) -> Unit,
    onUpdateParty: (PartyEntity) -> Unit,
    onDeleteParty: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val party = partyWithBalance?.party

    if (party == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Party not found.")
        }
        return
    }

    val isCustomer = party.type == PartyType.CUSTOMER.name
    val partyType = if (isCustomer) PartyType.CUSTOMER else PartyType.SUPPLIER

    var showAddTxDialog by remember { mutableStateOf(false) }
    var presetTxType by remember { mutableStateOf<TransactionType?>(null) }
    var showEditPartyDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedTxForDetail by remember { mutableStateOf<TransactionEntity?>(null) }
    var txToReverse by remember { mutableStateOf<TransactionEntity?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = party.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isCustomer) "Customer Ledger" else "Supplier Ledger",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ledger_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (party.phone.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${party.phone}"))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "Call", tint = PrimaryBlue)
                        }
                    }

                    IconButton(
                        onClick = {
                            shareStatement(context, party, partyWithBalance, ledger, currencySymbol)
                        },
                        modifier = Modifier.testTag("share_statement_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Statement")
                    }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Details") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showEditPartyDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Contact", color = MoneyOutRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MoneyOutRed) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Main Bottom Quick Action Buttons
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // GAVE CREDIT / BILL
                    Button(
                        onClick = {
                            presetTxType = if (isCustomer) TransactionType.CREDIT_SALE else TransactionType.CREDIT_PURCHASE
                            showAddTxDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("give_credit_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCustomer) "+ Gave Credit" else "+ Credit Bill",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // GOT PAYMENT / PAID
                    Button(
                        onClick = {
                            presetTxType = if (isCustomer) TransactionType.PAYMENT_RECEIVED else TransactionType.PAYMENT_MADE
                            showAddTxDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MoneyInGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("got_payment_button")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCustomer) "- Got Payment" else "- Made Payment",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Balance Overview Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NET OUTSTANDING BALANCE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            BalanceBadge(
                                balance = partyWithBalance.netBalance,
                                partyType = partyType,
                                currencySymbol = currencySymbol
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = formatCurrency(partyWithBalance.netBalance, currencySymbol),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (partyWithBalance.netBalance > 0) WarningAmber else if (partyWithBalance.netBalance < 0) MoneyInGreen else MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isCustomer) "Total Credit Given" else "Total Purchases",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCurrency(partyWithBalance.totalCredit, currencySymbol),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isCustomer) "Total Received" else "Total Paid",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatCurrency(partyWithBalance.totalPaid, currencySymbol),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MoneyInGreen
                                )
                            }
                        }
                    }
                }
            }

            // Opening Balance row if applicable
            if (party.openingBalance != 0.0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Opening Balance",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = formatShortDate(party.openingBalanceDate),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formatCurrency(party.openingBalance, currencySymbol),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (party.openingBalance > 0) WarningAmber else MoneyInGreen
                                )
                            )
                        }
                    }
                }
            }

            // Ledger Entries Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ledger History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Tap any entry to view or reverse",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (ledger.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No transactions recorded yet.\nUse the action buttons below to record credit or payment.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(ledger.reversed(), key = { it.transaction.id }) { item ->
                    LedgerEntryRow(
                        item = item,
                        currency = currencySymbol,
                        onClick = { selectedTxForDetail = item.transaction }
                    )
                }
            }
        }
    }

    // Add Transaction Dialog
    if (showAddTxDialog) {
        AddTransactionDialog(
            party = party,
            initialType = presetTxType,
            currencySymbol = currencySymbol,
            onDismiss = { showAddTxDialog = false },
            onConfirm = { type, amount, refNo, desc, notes ->
                onRecordTransaction(type, amount, refNo, desc, notes)
                showAddTxDialog = false
            }
        )
    }

    // Edit Party Dialog
    if (showEditPartyDialog) {
        AddEditPartyDialog(
            partyToEdit = party,
            partyType = partyType,
            currencySymbol = currencySymbol,
            onDismiss = { showEditPartyDialog = false },
            onConfirm = { name, phone, address, notes, _ ->
                onUpdateParty(
                    party.copy(
                        name = name,
                        phone = phone,
                        address = address,
                        notes = notes
                    )
                )
                showEditPartyDialog = false
            }
        )
    }

    // Delete Party Confirm
    if (showDeleteConfirmDialog) {
        ConfirmActionDialog(
            title = "Delete ${party.name}?",
            message = "This will permanently remove this contact and all their ledger transactions.",
            confirmText = "Delete",
            isDestructive = true,
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                onDeleteParty(party.id)
                showDeleteConfirmDialog = false
                onBack()
            }
        )
    }

    // Transaction Details Dialog
    selectedTxForDetail?.let { tx ->
        TransactionDetailDialog(
            transaction = tx,
            currencySymbol = currencySymbol,
            onDismiss = { selectedTxForDetail = null },
            onReverseClick = {
                txToReverse = tx
                selectedTxForDetail = null
            }
        )
    }

    // Dedicated Reversal / Correction Dialog
    txToReverse?.let { tx ->
        ReverseTransactionDialog(
            transaction = tx,
            currencySymbol = currencySymbol,
            onDismiss = { txToReverse = null },
            onConfirmReversal = { reason ->
                onReverseTransaction(tx.id, reason)
                txToReverse = null
            }
        )
    }
}

@Composable
fun LedgerEntryRow(
    item: LedgerItem,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tx = item.transaction
    val typeEnum = TransactionType.fromString(tx.transactionType)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("ledger_row_${tx.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatDate(tx.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (tx.referenceNo.isNotBlank()) {
                        Text(
                            text = " • #${tx.referenceNo}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Running Balance at this point in ledger
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Bal: ${formatCurrency(item.runningBalance, currency)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.description.ifBlank { typeEnum.title },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (tx.isReversed) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (tx.notes.isNotBlank()) {
                        Text(
                            text = tx.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Amount column
                Column(horizontalAlignment = Alignment.End) {
                    if (item.debitAmount > 0) {
                        Text(
                            text = "+ ${formatCurrency(item.debitAmount, currency)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (tx.isReversed) Color(0xFF94A3B8) else WarningAmber,
                                textDecoration = if (tx.isReversed) TextDecoration.LineThrough else TextDecoration.None
                            )
                        )
                    }
                    if (item.creditAmount > 0) {
                        Text(
                            text = "- ${formatCurrency(item.creditAmount, currency)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (tx.isReversed) Color(0xFF94A3B8) else MoneyInGreen,
                                textDecoration = if (tx.isReversed) TextDecoration.LineThrough else TextDecoration.None
                            )
                        )
                    }
                }
            }

            // Audit badges
            if (tx.isReversed) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MoneyOutRedContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "REVERSED & NEGATED (Preserved in Audit Log)",
                        color = MoneyOutRed,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (tx.isReversal) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = WarningAmberContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "CORRECTION REVERSAL${if (!tx.reversalReason.isNullOrBlank()) ": ${tx.reversalReason}" else ""}",
                        color = Color(0xFF92400E),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

fun shareStatement(
    context: Context,
    party: PartyEntity,
    partyWithBalance: PartyWithBalance?,
    ledger: List<LedgerItem>,
    currency: String
) {
    val sb = StringBuilder()
    sb.appendLine("========================================")
    sb.appendLine("ACCOUNT STATEMENT: ${party.name}")
    if (party.phone.isNotBlank()) sb.appendLine("Phone: ${party.phone}")
    if (party.address.isNotBlank()) sb.appendLine("Address: ${party.address}")
    sb.appendLine("Generated: ${formatDate(System.currentTimeMillis())}")
    sb.appendLine("----------------------------------------")
    if (partyWithBalance != null) {
        val bal = partyWithBalance.netBalance
        val status = if (bal > 0) "Amount Owed (Due)" else if (bal < 0) "Advance Balance" else "Settled"
        sb.appendLine("CURRENT BALANCE: $currency${formatCurrency(bal, "")} ($status)")
    }
    sb.appendLine("----------------------------------------")
    sb.appendLine("TRANSACTION HISTORY:")

    if (party.openingBalance != 0.0) {
        sb.appendLine("Opening Balance: $currency${formatCurrency(party.openingBalance, "")}")
    }

    for (item in ledger) {
        val tx = item.transaction
        val d = formatShortDate(tx.date)
        val debitStr = if (item.debitAmount > 0) "+$currency${formatCurrency(item.debitAmount, "")}" else ""
        val creditStr = if (item.creditAmount > 0) "-$currency${formatCurrency(item.creditAmount, "")}" else ""
        val status = if (tx.isReversed) " [REVERSED]" else if (tx.isReversal) " [CORRECTION: ${tx.reversalReason}]" else ""
        sb.appendLine("$d | ${tx.description.ifBlank { tx.transactionType }} | $debitStr $creditStr | Bal: $currency${formatCurrency(item.runningBalance, "")}$status")
    }
    sb.appendLine("========================================")
    sb.appendLine("Managed via DuesLedger (Offline)")

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, sb.toString())
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Statement for ${party.name}"))
}
