package com.example.data.model

import com.example.data.database.BusinessEntity
import com.example.data.database.PartyEntity
import com.example.data.database.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject

data class AppBackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val appName: String = "DuesLedger",
    val businesses: List<BusinessEntity>,
    val parties: List<PartyEntity>,
    val transactions: List<TransactionEntity>
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("version", version)
        root.put("exportedAt", exportedAt)
        root.put("appName", appName)

        val bizArray = JSONArray()
        for (b in businesses) {
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("name", b.name)
            obj.put("businessType", b.businessType)
            obj.put("ownerName", b.ownerName)
            obj.put("phone", b.phone)
            obj.put("address", b.address)
            obj.put("currencySymbol", b.currencySymbol)
            obj.put("createdAt", b.createdAt)
            obj.put("isDefault", b.isDefault)
            bizArray.put(obj)
        }
        root.put("businesses", bizArray)

        val partyArray = JSONArray()
        for (p in parties) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("businessId", p.businessId)
            obj.put("type", p.type)
            obj.put("name", p.name)
            obj.put("phone", p.phone)
            obj.put("address", p.address)
            obj.put("notes", p.notes)
            obj.put("openingBalance", p.openingBalance)
            obj.put("openingBalanceDate", p.openingBalanceDate)
            obj.put("createdAt", p.createdAt)
            obj.put("updatedAt", p.updatedAt)
            obj.put("isArchived", p.isArchived)
            partyArray.put(obj)
        }
        root.put("parties", partyArray)

        val txArray = JSONArray()
        for (t in transactions) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("businessId", t.businessId)
            obj.put("partyId", t.partyId)
            obj.put("partyType", t.partyType)
            obj.put("transactionType", t.transactionType)
            obj.put("amount", t.amount)
            obj.put("date", t.date)
            obj.put("referenceNo", t.referenceNo)
            obj.put("description", t.description)
            obj.put("notes", t.notes)
            obj.put("isReversed", t.isReversed)
            if (t.reversedByTransactionId != null) {
                obj.put("reversedByTransactionId", t.reversedByTransactionId)
            }
            obj.put("isReversal", t.isReversal)
            if (t.reversalOfTransactionId != null) {
                obj.put("reversalOfTransactionId", t.reversalOfTransactionId)
            }
            if (t.reversalReason != null) {
                obj.put("reversalReason", t.reversalReason)
            }
            obj.put("createdAt", t.createdAt)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        return root.toString(2)
    }

    companion object {
        fun fromJsonString(jsonStr: String): AppBackupData {
            val root = JSONObject(jsonStr)
            val version = root.optInt("version", 1)
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
            val appName = root.optString("appName", "DuesLedger")

            val businesses = mutableListOf<BusinessEntity>()
            val bizArray = root.optJSONArray("businesses") ?: JSONArray()
            for (i in 0 until bizArray.length()) {
                val obj = bizArray.getJSONObject(i)
                businesses.add(
                    BusinessEntity(
                        id = obj.optLong("id", 0),
                        name = obj.getString("name"),
                        businessType = obj.optString("businessType", "Retail Shop"),
                        ownerName = obj.optString("ownerName", ""),
                        phone = obj.optString("phone", ""),
                        address = obj.optString("address", ""),
                        currencySymbol = obj.optString("currencySymbol", "$"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        isDefault = obj.optBoolean("isDefault", false)
                    )
                )
            }

            val parties = mutableListOf<PartyEntity>()
            val partyArray = root.optJSONArray("parties") ?: JSONArray()
            for (i in 0 until partyArray.length()) {
                val obj = partyArray.getJSONObject(i)
                parties.add(
                    PartyEntity(
                        id = obj.optLong("id", 0),
                        businessId = obj.getLong("businessId"),
                        type = obj.getString("type"),
                        name = obj.getString("name"),
                        phone = obj.optString("phone", ""),
                        address = obj.optString("address", ""),
                        notes = obj.optString("notes", ""),
                        openingBalance = obj.optDouble("openingBalance", 0.0),
                        openingBalanceDate = obj.optLong("openingBalanceDate", System.currentTimeMillis()),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isArchived = obj.optBoolean("isArchived", false)
                    )
                )
            }

            val transactions = mutableListOf<TransactionEntity>()
            val txArray = root.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                transactions.add(
                    TransactionEntity(
                        id = obj.optLong("id", 0),
                        businessId = obj.getLong("businessId"),
                        partyId = obj.getLong("partyId"),
                        partyType = obj.getString("partyType"),
                        transactionType = obj.getString("transactionType"),
                        amount = obj.getDouble("amount"),
                        date = obj.getLong("date"),
                        referenceNo = obj.optString("referenceNo", ""),
                        description = obj.optString("description", ""),
                        notes = obj.optString("notes", ""),
                        isReversed = obj.optBoolean("isReversed", false),
                        reversedByTransactionId = if (obj.has("reversedByTransactionId")) obj.getLong("reversedByTransactionId") else null,
                        isReversal = obj.optBoolean("isReversal", false),
                        reversalOfTransactionId = if (obj.has("reversalOfTransactionId")) obj.getLong("reversalOfTransactionId") else null,
                        reversalReason = if (obj.has("reversalReason")) obj.getString("reversalReason") else null,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            return AppBackupData(
                version = version,
                exportedAt = exportedAt,
                appName = appName,
                businesses = businesses,
                parties = parties,
                transactions = transactions
            )
        }
    }
}
