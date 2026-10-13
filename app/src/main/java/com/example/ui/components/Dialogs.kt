package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.BusinessEntity
import com.example.data.database.PartyEntity
import com.example.data.database.TransactionEntity
import com.example.data.model.PartyType
import com.example.data.model.TransactionType
import com.example.ui.theme.MoneyInGreen
import com.example.ui.theme.MoneyOutRed
import com.example.ui.theme.MoneyOutRedContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditPartyDialog(
    partyToEdit: PartyEntity? = null,
    partyType: PartyType,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, address: String, notes: String, openingBalance: Double) -> Unit
) {
    var name by remember { mutableStateOf(partyToEdit?.name ?: "") }
    var phone by remember { mutableStateOf(partyToEdit?.phone ?: "") }
    var address by remember { mutableStateOf(partyToEdit?.address ?: "") }
    var notes by remember { mutableStateOf(partyToEdit?.notes ?: "") }
    var openingBalanceText by remember {
        mutableStateOf(if (partyToEdit != null && partyToEdit.openingBalance != 0.0) partyToEdit.openingBalance.toString() else "")
    }
    // true = owes us / we owe supplier; false = advance
    var isOwedDirection by remember { mutableStateOf(partyToEdit == null || partyToEdit.openingBalance >= 0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val typeLabel = if (partyType == PartyType.CUSTOMER) "Customer" else "Supplier"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (partyToEdit == null) "Add New $typeLabel" else "Edit $typeLabel",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("$typeLabel Name *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("party_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("party_phone_input")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (partyToEdit == null) {
                    Text(
                        text = "Opening Balance",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )

                    OutlinedTextField(
                        value = openingBalanceText,
                        onValueChange = { openingBalanceText = it },
                        label = { Text("Amount ($currencySymbol)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("party_opening_balance_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isOwedDirection,
                            onClick = { isOwedDirection = true },
                            label = {
                                Text(if (partyType == PartyType.CUSTOMER) "Customer owes you" else "You owe supplier")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MoneyOutRedContainer,
                                selectedLabelColor = MoneyOutRed
                            )
                        )
                        FilterChip(
                            selected = !isOwedDirection,
                            onClick = { isOwedDirection = false },
                            label = { Text("Advance / Prepayment") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFD1FAE5),
                                selectedLabelColor = MoneyInGreen
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Tags") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Name is required"
                        return@Button
                    }
                    val rawBalance = openingBalanceText.toDoubleOrNull() ?: 0.0
                    val finalBalance = if (isOwedDirection) rawBalance else -rawBalance
                    onConfirm(name.trim(), phone.trim(), address.trim(), notes.trim(), finalBalance)
                },
                modifier = Modifier.testTag("save_party_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    party: PartyEntity,
    initialType: TransactionType? = null,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (
        type: TransactionType,
        amount: Double,
        referenceNo: String,
        description: String,
        notes: String
    ) -> Unit
) {
    val isCustomer = party.type == PartyType.CUSTOMER.name
    val availableTypes = remember(isCustomer) {
        if (isCustomer) {
            listOf(
                TransactionType.CREDIT_SALE,
                TransactionType.SERVICE_ON_CREDIT,
                TransactionType.PAYMENT_RECEIVED,
                TransactionType.CUSTOMER_RETURN,
                TransactionType.CUSTOMER_ADJUST_ADD,
                TransactionType.CUSTOMER_ADJUST_SUB
            )
        } else {
            listOf(
                TransactionType.CREDIT_PURCHASE,
                TransactionType.EXPENSE_ON_CREDIT,
                TransactionType.PAYMENT_MADE,
                TransactionType.SUPPLIER_RETURN,
                TransactionType.SUPPLIER_ADJUST_ADD,
                TransactionType.SUPPLIER_ADJUST_SUB
            )
        }
    }

    var selectedType by remember {
        mutableStateOf(initialType ?: if (isCustomer) TransactionType.CREDIT_SALE else TransactionType.CREDIT_PURCHASE)
    }
    var amountText by remember { mutableStateOf("") }
    var referenceNo by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quickDescriptionPresets = remember(selectedType) {
        when (selectedType) {
            TransactionType.CREDIT_SALE -> listOf("Invoice #", "Store goods", "Monthly credit", "Bulk order")
            TransactionType.PAYMENT_RECEIVED -> listOf("Cash received", "Bank transfer", "Online payment", "Cheque")
            TransactionType.CREDIT_PURCHASE -> listOf("Bill #", "Raw materials", "Stock replenishment")
            TransactionType.PAYMENT_MADE -> listOf("Cash payment", "Online transfer", "Vendor bill clear")
            TransactionType.CUSTOMER_RETURN, TransactionType.SUPPLIER_RETURN -> listOf("Damaged goods", "Excess return")
            else -> listOf("Opening correction", "Reconciliation")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Record Transaction",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "For: ${party.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.title, fontSize = 12.sp) },
                            colors = if (type.isCreditGivingOrBuying) {
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WarningAmberContainer,
                                    selectedLabelColor = Color(0xFF92400E)
                                )
                            } else {
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFD1FAE5),
                                    selectedLabelColor = MoneyInGreen
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($currencySymbol) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_amount_input")
                )

                OutlinedTextField(
                    value = referenceNo,
                    onValueChange = { referenceNo = it },
                    label = { Text("Bill / Invoice / Receipt #") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_reference_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_description_input")
                )

                // Quick presets
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickDescriptionPresets.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                description = preset
                            }
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMessage = "Please enter a valid positive amount."
                        return@Button
                    }
                    onConfirm(selectedType, amount, referenceNo, description, notes)
                },
                modifier = Modifier.testTag("confirm_tx_button")
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Prominent Reversal / Correction Dialog:
 * Allows user to enter a clear reason for the correction.
 * Explains that a counter-balancing reversal transaction will be logged
 * and original transaction will be preserved with a reversed status tag.
 */
@Composable
fun ReverseTransactionDialog(
    transaction: TransactionEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirmReversal: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val typeEnum = TransactionType.fromString(transaction.transactionType)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.padding(4.dp)
            )
        },
        title = {
            Text(
                text = "Reverse / Correct Transaction",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Original Entry Details",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Type: ${typeEnum.title}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Amount: ${formatCurrency(transaction.amount, currencySymbol)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Date: ${formatDate(transaction.date)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (transaction.referenceNo.isNotBlank()) {
                            Text(
                                text = "Ref: ${transaction.referenceNo}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Text(
                    text = "A new reversal entry will be created referencing ID #${transaction.id} that cancels out this amount. The original transaction stays in the audit trail but will be flagged as Reversed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        if (it.isNotBlank()) errorMessage = null
                    },
                    label = { Text("Reason for Reversal / Correction *") },
                    placeholder = { Text("e.g. Billed to wrong customer, wrong amount entered") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reversal_reason_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isBlank()) {
                        errorMessage = "Please enter a reason for the reversal."
                        return@Button
                    }
                    onConfirmReversal(reason.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = MoneyOutRed),
                modifier = Modifier.testTag("confirm_reversal_button")
            ) {
                Text("Confirm Reversal")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TransactionDetailDialog(
    transaction: TransactionEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onReverseClick: (() -> Unit)? = null
) {
    val typeEnum = TransactionType.fromString(transaction.transactionType)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Transaction Details",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Type:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = typeEnum.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Amount:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = formatCurrency(transaction.amount, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (typeEnum.isCreditGivingOrBuying) WarningAmber else MoneyInGreen
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Date:", style = MaterialTheme.typography.bodyMedium)
                    Text(formatDate(transaction.date), style = MaterialTheme.typography.bodyMedium)
                }

                if (transaction.referenceNo.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Reference #:", style = MaterialTheme.typography.bodyMedium)
                        Text(transaction.referenceNo, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (transaction.description.isNotBlank()) {
                    Column {
                        Text("Description:", style = MaterialTheme.typography.labelMedium)
                        Text(transaction.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (transaction.notes.isNotBlank()) {
                    Column {
                        Text("Notes:", style = MaterialTheme.typography.labelMedium)
                        Text(transaction.notes, style = MaterialTheme.typography.bodySmall)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Reversal / Audit status
                if (transaction.isReversed) {
                    Surface(
                        color = MoneyOutRedContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "STATUS: REVERSED",
                                color = MoneyOutRed,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (transaction.reversedByTransactionId != null) {
                                Text(
                                    text = "Reversed by Entry #${transaction.reversedByTransactionId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MoneyOutRed
                                )
                            }
                        }
                    }
                }

                if (transaction.isReversal) {
                    Surface(
                        color = WarningAmberContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "CORRECTION REVERSAL ENTRY",
                                color = Color(0xFF92400E),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (transaction.reversalOfTransactionId != null) {
                                Text(
                                    text = "Negates Original Entry #${transaction.reversalOfTransactionId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF92400E)
                                )
                            }
                            if (!transaction.reversalReason.isNullOrBlank()) {
                                Text(
                                    text = "Reason: ${transaction.reversalReason}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!transaction.isReversed && !transaction.isReversal && onReverseClick != null) {
                Button(
                    onClick = onReverseClick,
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                    modifier = Modifier.testTag("open_reversal_button")
                ) {
                    Text("Reverse / Correct")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (!transaction.isReversed && !transaction.isReversal && onReverseClick != null) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BusinessEditorDialog(
    businessToEdit: BusinessEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        type: String,
        owner: String,
        phone: String,
        address: String,
        currency: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(businessToEdit?.name ?: "") }
    var type by remember { mutableStateOf(businessToEdit?.businessType ?: "Retail Shop") }
    var owner by remember { mutableStateOf(businessToEdit?.ownerName ?: "") }
    var phone by remember { mutableStateOf(businessToEdit?.phone ?: "") }
    var address by remember { mutableStateOf(businessToEdit?.address ?: "") }
    var currency by remember { mutableStateOf(businessToEdit?.currencySymbol?.takeIf { it != "$" } ?: "Rs") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetTypes = listOf(
        "Retail Shop",
        "Grocery & Mart",
        "Wholesale & Distribution",
        "Pharmacy & Medical",
        "Workshop & Auto",
        "Clothing & Apparel",
        "Agriculture & Seeds",
        "Services & Agency",
        "Custom Business"
    )

    val presetCurrencies = listOf("Rs", "PKR", "$", "AED", "SAR", "€", "£", "₹")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (businessToEdit == null) "Create Business Profile" else "Edit Business Profile",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Business Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("business_name_input")
                )

                Text(
                    text = "Business Type",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetTypes.forEach { pType ->
                        FilterChip(
                            selected = type == pType,
                            onClick = { type = pType },
                            label = { Text(pType, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("Owner / Manager Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact Phone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Store Address / City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Currency Symbol",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetCurrencies.forEach { symbol ->
                        FilterChip(
                            selected = currency == symbol,
                            onClick = { currency = symbol },
                            label = { Text(symbol, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter business name"
                        return@Button
                    }
                    onConfirm(name.trim(), type.trim(), owner.trim(), phone.trim(), address.trim(), currency.trim())
                },
                modifier = Modifier.testTag("save_business_button")
            ) {
                Text("Save Business")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    isDestructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) ButtonDefaults.buttonColors(containerColor = MoneyOutRed) else ButtonDefaults.buttonColors()
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
