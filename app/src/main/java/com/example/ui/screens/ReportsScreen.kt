package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.BusinessEntity
import com.example.data.database.TransactionEntity
import com.example.data.model.DateRangeFilter
import com.example.data.model.DateRangePreset
import com.example.data.model.PartyType
import com.example.data.model.PartyWithBalance
import com.example.data.model.TransactionType
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.components.formatShortDate
import com.example.ui.theme.MoneyInGreen
import com.example.ui.theme.MoneyOutRed
import com.example.ui.theme.Navy800
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    currentBusiness: BusinessEntity?,
    allTransactions: List<TransactionEntity>,
    customers: List<PartyWithBalance>,
    suppliers: List<PartyWithBalance>,
    dateFilter: DateRangeFilter,
    onSelectDatePreset: (DateRangePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currency = currentBusiness?.currencySymbol ?: "$"

    // Filter transactions by selected date range
    val periodTransactions = remember(allTransactions, dateFilter) {
        allTransactions.filter { it.date in dateFilter.startMillis..dateFilter.endMillis }
    }

    // Compute period totals
    var periodCreditSales = 0.0
    var periodMoneyReceived = 0.0
    var periodCreditPurchases = 0.0
    var periodMoneyPaid = 0.0

    for (tx in periodTransactions) {
        if (tx.isReversed) continue // Do not double count reversed entries

        val type = TransactionType.fromString(tx.transactionType)
        when (type) {
            TransactionType.CREDIT_SALE, TransactionType.SERVICE_ON_CREDIT -> periodCreditSales += tx.amount
            TransactionType.PAYMENT_RECEIVED -> periodMoneyReceived += tx.amount
            TransactionType.CREDIT_PURCHASE, TransactionType.EXPENSE_ON_CREDIT -> periodCreditPurchases += tx.amount
            TransactionType.PAYMENT_MADE -> periodMoneyPaid += tx.amount
            else -> {}
        }
    }

    val netCashflow = periodMoneyReceived - periodMoneyPaid

    val topCustomerDebtors = remember(customers) {
        customers.filter { it.netBalance > 0 }.sortedByDescending { it.netBalance }.take(5)
    }

    val topSupplierCreditors = remember(suppliers) {
        suppliers.filter { it.netBalance > 0 }.sortedByDescending { it.netBalance }.take(5)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Presets Filter Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Report Period",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dateFilter.preset.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DateRangePreset.entries.filter { it != DateRangePreset.CUSTOM }.forEach { preset ->
                            FilterChip(
                                selected = dateFilter.preset == preset,
                                onClick = { onSelectDatePreset(preset) },
                                label = { Text(preset.label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Period Summary Overview Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PERIOD CASHFLOW SUMMARY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Net Cash Flow",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(netCashflow, currency),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = if (netCashflow >= 0) MoneyInGreen else MoneyOutRed
                                )
                            )
                        }

                        Button(
                            onClick = {
                                shareFullReport(
                                    context = context,
                                    business = currentBusiness,
                                    filter = dateFilter,
                                    periodCreditSales = periodCreditSales,
                                    periodMoneyReceived = periodMoneyReceived,
                                    periodCreditPurchases = periodCreditPurchases,
                                    periodMoneyPaid = periodMoneyPaid,
                                    customers = customers,
                                    suppliers = suppliers,
                                    currency = currency
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("export_report_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Report")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Credit Sales", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = formatCurrency(periodCreditSales, currency), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column {
                            Text(text = "Received (Cash In)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = formatCurrency(periodMoneyReceived, currency), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MoneyInGreen)
                        }
                        Column {
                            Text(text = "Purchases", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = formatCurrency(periodCreditPurchases, currency), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Paid (Cash Out)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = formatCurrency(periodMoneyPaid, currency), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MoneyOutRed)
                        }
                    }
                }
            }
        }

        // Top Customer Receivables Report
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customer Outstanding Dues",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${customers.count { it.netBalance > 0 }} debtors",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (topCustomerDebtors.isEmpty()) {
                        Text(
                            text = "No pending customer dues.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        topCustomerDebtors.forEach { debtor ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = debtor.party.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    if (debtor.party.phone.isNotBlank()) {
                                        Text(
                                            text = debtor.party.phone,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = formatCurrency(debtor.netBalance, currency),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = WarningAmber
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Supplier Payables Report
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Supplier Outstanding Payables",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${suppliers.count { it.netBalance > 0 }} creditors",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (topSupplierCreditors.isEmpty()) {
                        Text(
                            text = "No pending supplier payables.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        topSupplierCreditors.forEach { creditor ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = creditor.party.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    if (creditor.party.phone.isNotBlank()) {
                                        Text(
                                            text = creditor.party.phone,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = formatCurrency(creditor.netBalance, currency),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MoneyOutRed
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun shareFullReport(
    context: Context,
    business: BusinessEntity?,
    filter: DateRangeFilter,
    periodCreditSales: Double,
    periodMoneyReceived: Double,
    periodCreditPurchases: Double,
    periodMoneyPaid: Double,
    customers: List<PartyWithBalance>,
    suppliers: List<PartyWithBalance>,
    currency: String
) {
    val totalCustDues = customers.filter { it.netBalance > 0 }.sumOf { it.netBalance }
    val totalSuppDues = suppliers.filter { it.netBalance > 0 }.sumOf { it.netBalance }

    val sb = StringBuilder()
    sb.appendLine("========================================")
    sb.appendLine("BUSINESS FINANCIAL REPORT: ${business?.name ?: "DuesLedger"}")
    sb.appendLine("Type: ${business?.businessType ?: "General"}")
    sb.appendLine("Period: ${filter.preset.label}")
    sb.appendLine("Date: ${formatDate(System.currentTimeMillis())}")
    sb.appendLine("----------------------------------------")
    sb.appendLine("PERIOD CASHFLOW & ACTIVITY:")
    sb.appendLine("Credit Sales: $currency${formatCurrency(periodCreditSales, "")}")
    sb.appendLine("Cash/Payment Received: $currency${formatCurrency(periodMoneyReceived, "")}")
    sb.appendLine("Credit Purchases: $currency${formatCurrency(periodCreditPurchases, "")}")
    sb.appendLine("Cash/Payment Made: $currency${formatCurrency(periodMoneyPaid, "")}")
    sb.appendLine("Net Cash Flow: $currency${formatCurrency(periodMoneyReceived - periodMoneyPaid, "")}")
    sb.appendLine("----------------------------------------")
    sb.appendLine("TOTAL OUTSTANDING SUMMARY:")
    sb.appendLine("Total Customer Receivables: $currency${formatCurrency(totalCustDues, "")}")
    sb.appendLine("Total Supplier Payables: $currency${formatCurrency(totalSuppDues, "")}")
    sb.appendLine("Net Position: $currency${formatCurrency(totalCustDues - totalSuppDues, "")}")
    sb.appendLine("----------------------------------------")
    sb.appendLine("TOP CUSTOMER DUES:")
    customers.filter { it.netBalance > 0 }.sortedByDescending { it.netBalance }.take(10).forEach {
        sb.appendLine("- ${it.party.name}: $currency${formatCurrency(it.netBalance, "")}")
    }
    sb.appendLine("----------------------------------------")
    sb.appendLine("TOP SUPPLIER PAYABLES:")
    suppliers.filter { it.netBalance > 0 }.sortedByDescending { it.netBalance }.take(10).forEach {
        sb.appendLine("- ${it.party.name}: $currency${formatCurrency(it.netBalance, "")}")
    }
    sb.appendLine("========================================")
    sb.appendLine("Generated by DuesLedger (Offline)")

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, sb.toString())
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Financial Report"))
}
