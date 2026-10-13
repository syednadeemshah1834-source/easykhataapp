package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.PartyEntity
import com.example.data.model.PartyType
import com.example.data.model.PartyWithBalance
import com.example.ui.components.AddEditPartyDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PartyListItem
import com.example.ui.components.SearchBarField
import com.example.ui.components.formatCurrency
import com.example.ui.theme.MoneyInGreen
import com.example.ui.theme.MoneyOutRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.WarningAmber
import kotlin.math.abs

enum class PartyFilterTab(val label: String) {
    ALL("All"),
    HAS_DUES("Has Dues"),
    SETTLED("Settled"),
    ADVANCE("Advance")
}

@Composable
fun PartiesScreen(
    partyType: PartyType,
    parties: List<PartyWithBalance>,
    currencySymbol: String,
    onPartyClick: (partyId: Long) -> Unit,
    onAddParty: (name: String, phone: String, address: String, notes: String, openingBalance: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(PartyFilterTab.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }

    val isCustomer = partyType == PartyType.CUSTOMER
    val typeTitle = if (isCustomer) "Customers" else "Suppliers"

    // Filter logic
    val filteredParties = remember(parties, searchQuery, selectedFilter) {
        parties.filter { item ->
            val p = item.party
            val matchesQuery = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.phone.contains(searchQuery, ignoreCase = true) ||
                p.address.contains(searchQuery, ignoreCase = true) ||
                p.notes.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (selectedFilter) {
                PartyFilterTab.ALL -> true
                PartyFilterTab.HAS_DUES -> item.netBalance > 0.01
                PartyFilterTab.SETTLED -> abs(item.netBalance) <= 0.01
                PartyFilterTab.ADVANCE -> item.netBalance < -0.01
            }

            matchesQuery && matchesTab
        }
    }

    val totalDues = remember(parties) {
        parties.filter { it.netBalance > 0 }.sumOf { it.netBalance }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_party_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add $typeTitle")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Outstanding Dues Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCustomer) WarningAmber.copy(alpha = 0.12f) else MoneyOutRed.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isCustomer) "TOTAL CUSTOMER RECEIVABLES" else "TOTAL SUPPLIER PAYABLES",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isCustomer) WarningAmber else MoneyOutRed
                        )
                        Text(
                            text = formatCurrency(totalDues, currencySymbol),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = if (isCustomer) WarningAmber else MoneyOutRed
                        )
                    }

                    Text(
                        text = "${parties.count { it.netBalance > 0 }} active dues",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search Bar & Filters
            Column(modifier = Modifier.padding(16.dp)) {
                SearchBarField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search by name, phone, address..."
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PartyFilterTab.entries.forEach { tab ->
                        FilterChip(
                            selected = selectedFilter == tab,
                            onClick = { selectedFilter = tab },
                            label = { Text(tab.label, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // List of Parties
            if (filteredParties.isEmpty()) {
                EmptyStateView(
                    icon = if (isCustomer) Icons.Default.Group else Icons.Default.Inventory2,
                    title = if (searchQuery.isNotBlank()) "No matching records found" else "No $typeTitle yet",
                    message = if (searchQuery.isNotBlank()) "Try searching with a different name or phone number."
                    else "Add your first $typeTitle to keep track of credit sales, payments, and balances.",
                    actionLabel = if (searchQuery.isBlank()) "Add $typeTitle" else null,
                    onAction = { showAddDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredParties, key = { it.party.id }) { partyWithBal ->
                        PartyListItem(
                            partyWithBalance = partyWithBal,
                            partyType = partyType,
                            currencySymbol = currencySymbol,
                            onClick = { onPartyClick(partyWithBal.party.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditPartyDialog(
            partyType = partyType,
            currencySymbol = currencySymbol,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, address, notes, openingBal ->
                onAddParty(name, phone, address, notes, openingBal)
                showAddDialog = false
            }
        )
    }
}
