package com.shohankhan.hisaabdesk.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.hisaabdesk.AppContainer
import com.shohankhan.hisaabdesk.data.local.entity.CategoryEntity
import com.shohankhan.hisaabdesk.data.local.entity.CategoryKind
import com.shohankhan.hisaabdesk.ui.appViewModel
import com.shohankhan.hisaabdesk.ui.components.DropdownField
import com.shohankhan.hisaabdesk.ui.components.EmptyState
import com.shohankhan.hisaabdesk.ui.components.HisaabScaffold
import com.shohankhan.hisaabdesk.ui.components.LabeledField
import com.shohankhan.hisaabdesk.ui.components.SectionCard
import com.shohankhan.hisaabdesk.ui.components.SoftDivider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

fun categoryKindLabel(kind: CategoryKind): String = when (kind) {
    CategoryKind.INCOME -> "আয়"
    CategoryKind.EXPENSE -> "ব্যয়"
    CategoryKind.SAVINGS -> "সঞ্চয়"
    CategoryKind.BORROWING -> "ধার নেওয়া"
    CategoryKind.LENDING -> "ধার দেওয়া"
    CategoryKind.RECEIVABLE -> "পাওনা"
    CategoryKind.PAYABLE -> "দেনা"
    CategoryKind.LOAN -> "ঋণ"
    CategoryKind.SHOPPING -> "কেনাকাটা"
    CategoryKind.BILL -> "বিল"
    CategoryKind.OTHER -> "অন্যান্য"
}

private val presetColors = listOf(
    0xFF18794E, 0xFF125C3B, 0xFFBA3030, 0xFF98620B, 0xFF2D5B8A,
    0xFF6B3FA0, 0xFFB3541E, 0xFF0F766E, 0xFF7A5C2E, 0xFF444444
)

class CategoryViewModel(private val container: AppContainer) : ViewModel() {
    val categories: StateFlow<List<CategoryEntity>> = container.categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(category: CategoryEntity) {
        viewModelScope.launch { container.categoryRepository.save(category) }
    }

    fun setArchived(id: Long, archived: Boolean) {
        viewModelScope.launch { container.categoryRepository.setArchived(id, archived) }
    }

    fun move(category: CategoryEntity, up: Boolean) {
        viewModelScope.launch {
            val siblings = categories.value.filter { it.kind == category.kind && it.parentId == category.parentId }
                .sortedBy { it.sortOrder }
            container.categoryRepository.move(category, up, siblings)
        }
    }

    fun delete(category: CategoryEntity) {
        viewModelScope.launch { container.categoryRepository.delete(category) }
    }

    suspend fun get(id: Long) = container.categoryRepository.get(id)
}

@Composable
fun CategoryListScreen(onBack: () -> Unit, onAdd: () -> Unit, onEdit: (Long) -> Unit) {
    val vm: CategoryViewModel = appViewModel { CategoryViewModel(it.container) }
    val categories by vm.categories.collectAsState()
    var kindFilter by remember { mutableStateOf<CategoryKind?>(null) }

    val filtered = categories.filter { kindFilter == null || it.kind == kindFilter }

    HisaabScaffold(title = "ক্যাটাগরি ব্যবস্থাপনা", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = kindFilter == null, onClick = { kindFilter = null }, label = { Text("সব") })
            FilterChip(selected = kindFilter == CategoryKind.INCOME, onClick = { kindFilter = CategoryKind.INCOME }, label = { Text("আয়") })
            FilterChip(selected = kindFilter == CategoryKind.EXPENSE, onClick = { kindFilter = CategoryKind.EXPENSE }, label = { Text("ব্যয়") })
        }
        Spacer(Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            EmptyState("কোনো ক্যাটাগরি নেই", actionLabel = "যোগ করুন", onAction = onAdd)
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.id }) { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEdit(cat.id) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(
                            Modifier
                                .size(14.dp)
                                .background(Color(cat.colorArgb.toULong()), CircleShape)
                        )
                        Spacer(Modifier.size(10.dp))
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                            Text(
                                cat.name + if (cat.isArchived) " (সংরক্ষণাগারভুক্ত)" else "",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                categoryKindLabel(cat.kind) + if (cat.isSystemSuggestion) " · প্রস্তাবিত" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { vm.move(cat, up = true) }) { Text("↑") }
                        TextButton(onClick = { vm.move(cat, up = false) }) { Text("↓") }
                    }
                    SoftDivider()
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("ক্যাটাগরি যোগ করুন") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun CategoryFormScreen(id: Long, onBack: () -> Unit, onSaved: () -> Unit) {
    val vm: CategoryViewModel = appViewModel { CategoryViewModel(it.container) }
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(CategoryKind.EXPENSE) }
    var color by remember { mutableStateOf(presetColors.first()) }
    var parent by remember { mutableStateOf<Long?>(null) }
    var prefilled by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val categories by vm.categories.collectAsState()

    LaunchedEffect(id) {
        if (id != 0L && !prefilled) {
            val cat = vm.get(id)
            if (cat != null) {
                prefilled = true
                name = cat.name
                kind = cat.kind
                color = cat.colorArgb
                parent = cat.parentId
            }
        }
    }

    HisaabScaffold(title = if (id == 0L) "ক্যাটাগরি যোগ করুন" else "ক্যাটাগরি সম্পাদনা", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        error?.let { com.shohankhan.hisaabdesk.ui.components.ErrorBanner(it); Spacer(Modifier.height(8.dp)) }
        LabeledField("ক্যাটাগরির নাম", name, { name = it })
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = "ক্যাটাগরির ধরন",
            options = CategoryKind.entries,
            selected = kind,
            optionLabel = { categoryKindLabel(it) },
            onSelected = { kind = it }
        )
        Spacer(Modifier.height(8.dp))
        Text("রং", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presetColors.forEach { c ->
                Spacer(
                    Modifier
                        .size(32.dp)
                        .background(Color(c), CircleShape)
                        .clickable { color = c }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        val parentOptions = categories.filter { it.parentId == null && it.id != id && it.kind == kind }
        DropdownField(
            label = "প্যারেন্ট ক্যাটাগরি (ঐচ্ছিক)",
            options = parentOptions,
            selected = parentOptions.firstOrNull { it.id == parent },
            optionLabel = { it.name },
            onSelected = { parent = it.id }
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (name.isBlank()) {
                    error = "ক্যাটাগরির নাম দিন"
                } else {
                    error = null
                    val now = System.currentTimeMillis()
                    val existing = if (id != 0L) vm.get(id) else null
                    vm.save(
                        (existing ?: CategoryEntity(name = name.trim(), kind = kind, createdAt = now, updatedAt = now)).copy(
                            name = name.trim(),
                            kind = kind,
                            colorArgb = color,
                            parentId = parent,
                            updatedAt = now
                        )
                    )
                    onSaved()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("সংরক্ষণ") }
        if (id != 0L) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val now = System.currentTimeMillis()
                    val existing = categories.firstOrNull { it.id == id }
                    if (existing != null) vm.setArchived(id, !existing.isArchived)
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (categories.firstOrNull { it.id == id }?.isArchived == true) "ফিরিয়ে আনুন" else "সংরক্ষণাগারে রাখুন")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
