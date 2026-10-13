package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.database.BusinessEntity
import com.example.ui.components.BusinessEditorDialog
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.theme.MoneyInGreen
import com.example.ui.theme.MoneyOutRed
import com.example.ui.theme.Navy800
import com.example.ui.theme.PrimaryBlue

@Composable
fun BusinessesScreen(
    businesses: List<BusinessEntity>,
    activeBusinessId: Long?,
    onSelectBusiness: (Long) -> Unit,
    onCreateBusiness: (
        name: String,
        type: String,
        owner: String,
        phone: String,
        address: String,
        currency: String
    ) -> Unit,
    onUpdateBusiness: (BusinessEntity) -> Unit,
    onDeleteBusiness: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var businessToEdit by remember { mutableStateOf<BusinessEntity?>(null) }
    var businessToDelete by remember { mutableStateOf<BusinessEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_business_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Business")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("businesses_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Manage Businesses",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Switch between businesses on this device. Each business keeps completely isolated customers, suppliers, transactions, and reports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(businesses, key = { it.id }) { biz ->
                val isActive = biz.id == activeBusinessId

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) PrimaryBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 3.dp else 1.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectBusiness(biz.id) }
                        .testTag("business_card_${biz.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isActive) PrimaryBlue else PrimaryBlue.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Store,
                                        contentDescription = null,
                                        tint = if (isActive) Color.White else PrimaryBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = biz.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${biz.businessType} • Currency: ${biz.currencySymbol}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isActive) {
                                Surface(
                                    color = MoneyInGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Active Business",
                                            tint = MoneyInGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ACTIVE",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MoneyInGreen
                                        )
                                    }
                                }
                            }
                        }

                        if (biz.ownerName.isNotBlank() || biz.phone.isNotBlank() || biz.address.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = listOfNotNull(
                                    biz.ownerName.takeIf { it.isNotBlank() }?.let { "Owner: $it" },
                                    biz.phone.takeIf { it.isNotBlank() },
                                    biz.address.takeIf { it.isNotBlank() }
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isActive) {
                                Button(
                                    onClick = { onSelectBusiness(biz.id) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Switch to this")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            IconButton(onClick = { businessToEdit = biz }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Business")
                            }

                            if (businesses.size > 1) {
                                IconButton(onClick = { businessToDelete = biz }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Business",
                                        tint = MoneyOutRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        BusinessEditorDialog(
            businessToEdit = null,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, type, owner, phone, address, currency ->
                onCreateBusiness(name, type, owner, phone, address, currency)
                showCreateDialog = false
            }
        )
    }

    businessToEdit?.let { biz ->
        BusinessEditorDialog(
            businessToEdit = biz,
            onDismiss = { businessToEdit = null },
            onConfirm = { name, type, owner, phone, address, currency ->
                onUpdateBusiness(
                    biz.copy(
                        name = name,
                        businessType = type,
                        ownerName = owner,
                        phone = phone,
                        address = address,
                        currencySymbol = currency
                    )
                )
                businessToEdit = null
            }
        )
    }

    businessToDelete?.let { biz ->
        ConfirmActionDialog(
            title = "Delete Business: ${biz.name}?",
            message = "This will permanently remove this business profile along with all its customers, suppliers, and transactions. This cannot be undone.",
            confirmText = "Delete Business",
            isDestructive = true,
            onDismiss = { businessToDelete = null },
            onConfirm = {
                onDeleteBusiness(biz.id)
                businessToDelete = null
            }
        )
    }
}
