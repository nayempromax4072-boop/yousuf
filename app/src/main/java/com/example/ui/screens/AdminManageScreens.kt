package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Announcement
import com.example.data.model.BusinessSettings
import com.example.data.model.ServiceCategory
import com.example.data.model.UserProfile
import com.example.localization.AppLanguage
import com.example.localization.LocalStrings
import com.example.ui.components.PrimaryYellowButton
import com.example.ui.components.categoryIconForKey
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricYellow
import com.example.ui.theme.LightGray
import com.example.ui.theme.SubtleBorder

@Composable
fun AdminManageTab(
    language: AppLanguage,
    categories: List<ServiceCategory>,
    customers: List<UserProfile>,
    announcements: List<Announcement>,
    businessSettings: BusinessSettings,
    onSaveCategory: (ServiceCategory, Boolean) -> Unit,
    onToggleCategory: (String, Boolean) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSendTargetedNotification: (String, String, String) -> Unit,
    onSendAnnouncement: (String, String) -> Unit,
    onSaveBusinessSettings: (BusinessSettings) -> Unit
) {
    val strings = LocalStrings.current
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf(strings.manageCategories, strings.sendNotification, strings.businessSettings)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = CharcoalSurface,
            contentColor = ElectricYellow,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = ElectricYellow
                )
            }
        ) {
            subTabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedSubTab == idx,
                    onClick = { selectedSubTab = idx },
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            color = if (selectedSubTab == idx) ElectricYellow else LightGray
                        )
                    }
                )
            }
        }

        when (selectedSubTab) {
            0 -> AdminCategoriesSection(
                language = language,
                categories = categories,
                onSaveCategory = onSaveCategory,
                onToggleCategory = onToggleCategory,
                onDeleteCategory = onDeleteCategory
            )
            1 -> AdminNotificationsSection(
                customers = customers.filter { it.role != "admin" },
                announcements = announcements,
                onSendTargeted = onSendTargetedNotification,
                onSendAnnouncement = onSendAnnouncement
            )
            2 -> AdminBusinessSettingsSection(
                settings = businessSettings,
                onSave = onSaveBusinessSettings
            )
        }
    }
}

@Composable
private fun AdminCategoriesSection(
    language: AppLanguage,
    categories: List<ServiceCategory>,
    onSaveCategory: (ServiceCategory, Boolean) -> Unit,
    onToggleCategory: (String, Boolean) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    val strings = LocalStrings.current
    var editingCategory by remember { mutableStateOf<ServiceCategory?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        PrimaryYellowButton(
            text = strings.addCategory,
            icon = Icons.Filled.Add,
            onClick = {
                isCreatingNew = true
                editingCategory = ServiceCategory(
                    sortOrder = (categories.size + 1).toLong(),
                    isActive = true,
                    iconKey = "other"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        categories.forEach { cat ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                border = BorderStroke(1.dp, SubtleBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(
                        imageVector = categoryIconForKey(cat.iconKey),
                        contentDescription = null,
                        tint = ElectricYellow,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cat.localizedName(language), style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                    Switch(
                        checked = cat.isActive,
                        onCheckedChange = { onToggleCategory(cat.id, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = DeepBlack, checkedTrackColor = ElectricYellow)
                    )
                    IconButton(onClick = {
                        isCreatingNew = false
                        editingCategory = cat
                    }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = ElectricYellow)
                    }
                    IconButton(onClick = { onDeleteCategory(cat.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = DangerRed)
                    }
                }
            }
        }
    }

    editingCategory?.let { cat ->
        var nameEn by remember(cat.id) { mutableStateOf(cat.nameEn) }
        var nameBn by remember(cat.id) { mutableStateOf(cat.nameBn) }
        var descEn by remember(cat.id) { mutableStateOf(cat.descriptionEn) }
        var descBn by remember(cat.id) { mutableStateOf(cat.descriptionBn) }
        var iconKey by remember(cat.id) { mutableStateOf(cat.iconKey) }
        val iconKeys = listOf("fan", "motor", "ac", "light", "wiring", "cctv", "plumbing", "building_electric", "maintenance", "other")

        AlertDialog(
            onDismissRequest = { editingCategory = null },
            containerColor = CharcoalSurface,
            title = { Text(if (isCreatingNew) strings.addCategory else strings.editCategory, color = ElectricYellow) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("Name (English)") })
                    OutlinedTextField(value = nameBn, onValueChange = { nameBn = it }, label = { Text("Name (Bengali)") })
                    OutlinedTextField(value = descEn, onValueChange = { descEn = it }, label = { Text("Description (English)") }, minLines = 2)
                    OutlinedTextField(value = descBn, onValueChange = { descBn = it }, label = { Text("Description (Bengali)") }, minLines = 2)
                    Text("Icon", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        iconKeys.forEach { key ->
                            FilterChip(
                                selected = iconKey == key,
                                onClick = { iconKey = key },
                                label = { Text(key) },
                                leadingIcon = {
                                    Icon(categoryIconForKey(key), contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricYellow,
                                    selectedLabelColor = DeepBlack,
                                    selectedLeadingIconColor = DeepBlack
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameEn.isNotBlank() && descEn.isNotBlank()) {
                            onSaveCategory(
                                cat.copy(
                                    nameEn = nameEn,
                                    nameBn = nameBn.ifBlank { nameEn },
                                    descriptionEn = descEn,
                                    descriptionBn = descBn.ifBlank { descEn },
                                    estimatedPriceBdt = "",
                                    iconKey = iconKey
                                ),
                                isCreatingNew
                            )
                            editingCategory = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricYellow, contentColor = DeepBlack)
                ) {
                    Text(strings.saveChanges, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text(strings.cancel, color = LightGray)
                }
            }
        )
    }
}

@Composable
private fun AdminNotificationsSection(
    customers: List<UserProfile>,
    announcements: List<Announcement>,
    onSendTargeted: (String, String, String) -> Unit,
    onSendAnnouncement: (String, String) -> Unit
) {
    val strings = LocalStrings.current
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastBody by remember { mutableStateOf("") }

    var selectedCustomer by remember { mutableStateOf(customers.firstOrNull()) }
    var customerMenuOpen by remember { mutableStateOf(false) }
    var directTitle by remember { mutableStateOf("") }
    var directBody by remember { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Broadcast Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            border = BorderStroke(1.dp, ElectricYellow.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Campaign, contentDescription = null, tint = ElectricYellow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.broadcastAll, style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                OutlinedTextField(
                    value = broadcastTitle,
                    onValueChange = { broadcastTitle = it },
                    label = { Text("Announcement Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = broadcastBody,
                    onValueChange = { broadcastBody = it },
                    label = { Text("Announcement Message") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryYellowButton(
                    text = "Send Announcement",
                    onClick = {
                        if (broadcastTitle.isNotBlank() && broadcastBody.isNotBlank()) {
                            onSendAnnouncement(broadcastTitle, broadcastBody)
                            broadcastTitle = ""
                            broadcastBody = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Targeted Customer Notification Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            border = BorderStroke(1.dp, SubtleBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = ElectricYellow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Notify Individual Customer", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                Box {
                    OutlinedButton(
                        onClick = { customerMenuOpen = true },
                        border = BorderStroke(1.dp, SubtleBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            selectedCustomer?.let { "${it.fullName} (${it.phone.ifBlank { it.email }})" }
                                ?: "Select Customer",
                            color = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = customerMenuOpen,
                        onDismissRequest = { customerMenuOpen = false },
                        modifier = Modifier.background(CharcoalSurface)
                    ) {
                        customers.forEach { cust ->
                            DropdownMenuItem(
                                text = { Text("${cust.fullName} • ${cust.phone}", color = Color.White) },
                                onClick = {
                                    selectedCustomer = cust
                                    customerMenuOpen = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = directTitle,
                    onValueChange = { directTitle = it },
                    label = { Text("Notification Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = directBody,
                    onValueChange = { directBody = it },
                    label = { Text("Notification Message") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryYellowButton(
                    text = "Send to Customer",
                    onClick = {
                        val target = selectedCustomer
                        if (target != null && directTitle.isNotBlank() && directBody.isNotBlank()) {
                            onSendTargeted(target.uid, directTitle, directBody)
                            directTitle = ""
                            directBody = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (announcements.isNotEmpty()) {
            Text("Recent Announcements", style = MaterialTheme.typography.titleSmall, color = LightGray)
            announcements.take(5).forEach { ann ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    border = BorderStroke(1.dp, SubtleBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(ann.title, style = MaterialTheme.typography.titleSmall, color = ElectricYellow)
                        Text(ann.body, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text(formatTimestamp(ann.createdAt), style = MaterialTheme.typography.labelSmall, color = LightGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminBusinessSettingsSection(
    settings: BusinessSettings,
    onSave: (BusinessSettings) -> Unit
) {
    val strings = LocalStrings.current
    var shopName by remember(settings.shopName) { mutableStateOf(settings.shopName) }
    var tagline by remember(settings.tagline) { mutableStateOf(settings.tagline) }
    var phone by remember(settings.phone) { mutableStateOf(settings.phone) }
    var email by remember(settings.email) { mutableStateOf(settings.email) }
    var address by remember(settings.address) { mutableStateOf(settings.address) }
    var description by remember(settings.description) { mutableStateOf(settings.description) }
    var workingHours by remember(settings.workingHours) { mutableStateOf(settings.workingHours) }
    var isAvailable by remember(settings.isServiceAvailable) { mutableStateOf(settings.isServiceAvailable) }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Store, contentDescription = null, tint = ElectricYellow)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.businessSettings, style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(CharcoalSurface, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text("Service Availability (Accepting Orders)", color = Color.White)
            Switch(
                checked = isAvailable,
                onCheckedChange = { isAvailable = it },
                colors = SwitchDefaults.colors(checkedThumbColor = DeepBlack, checkedTrackColor = ElectricYellow)
            )
        }

        OutlinedTextField(value = shopName, onValueChange = { shopName = it }, label = { Text("Shop Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Tagline") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Contact Number") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Shop Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = workingHours, onValueChange = { workingHours = it }, label = { Text("Working Hours") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Business Address") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Business Description") }, minLines = 3, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(6.dp))
        PrimaryYellowButton(
            text = strings.saveChanges,
            onClick = {
                onSave(
                    settings.copy(
                        shopName = shopName,
                        tagline = tagline,
                        phone = phone,
                        email = email,
                        address = address,
                        description = description,
                        workingHours = workingHours,
                        isServiceAvailable = isAvailable
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}
