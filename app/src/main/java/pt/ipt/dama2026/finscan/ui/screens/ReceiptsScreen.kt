package pt.ipt.dama2026.finscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.ipt.dama2026.finscan.R
import pt.ipt.dama2026.finscan.data.api.ApiClient
import pt.ipt.dama2026.finscan.data.api.models.CategoryResponse
import pt.ipt.dama2026.finscan.data.api.models.ReceiptCreateRequest
import pt.ipt.dama2026.finscan.data.api.models.ReceiptResponse
import pt.ipt.dama2026.finscan.data.api.models.ReceiptUpdateRequest
import pt.ipt.dama2026.finscan.data.api.services.CategoryApiService
import pt.ipt.dama2026.finscan.data.api.services.ReceiptApiService
import pt.ipt.dama2026.finscan.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val api = remember { ApiClient.getRetrofit().create(ReceiptApiService::class.java) }
    val catApi = remember { ApiClient.getRetrofit().create(CategoryApiService::class.java) }

    val PAGE_SIZE = 10

    var receipts by remember { mutableStateOf(listOf<ReceiptResponse>()) }
    var categories by remember { mutableStateOf(listOf<CategoryResponse>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(1) }
    var hasNextPage by remember { mutableStateOf(false) }
    var isFirstRender by remember { mutableStateOf(true) }
    var showCategories by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    var receiptToDelete by remember { mutableStateOf<ReceiptResponse?>(null) }
    var receiptToEdit by remember { mutableStateOf<ReceiptResponse?>(null) }
    var receiptToView by remember { mutableStateOf<ReceiptResponse?>(null) }

    suspend fun loadCategories() {
        try {
            val resp = catApi.listCategories(skip = 0, limit = 100)
            if (resp.isSuccessful) {
                categories = (resp.body() ?: emptyList()).sortedBy { it.name.lowercase() }
            }
        } catch (_: Exception) {}
    }

    suspend fun loadPage(page: Int) {
        if (isLoading) return
        isLoading = true
        errorMessage = ""
        try {
            val response = api.listReceipts(
                search = searchQuery.ifBlank { null },
                skip = (page - 1) * PAGE_SIZE,
                limit = PAGE_SIZE + 1
            )
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                hasNextPage = body.size > PAGE_SIZE
                receipts = if (hasNextPage) body.dropLast(1) else body
            } else {
                errorMessage = if (response.code() >= 500) {
                    context.getString(R.string.auth_unknown_error)
                } else {
                    context.getString(R.string.receipts_load_error)
                }
            }
        } catch (e: Exception) {
            errorMessage = context.getString(R.string.auth_error_internet)
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadCategories()
        loadPage(1)
    }

    LaunchedEffect(searchQuery) {
        if (isFirstRender) {
            isFirstRender = false
            return@LaunchedEffect
        }
        delay(400)
        currentPage = 1
        loadPage(1)
    }

    if (showCategories) {
        CategoriesScreen(onBack = {
            showCategories = false
            scope.launch { loadCategories() }
        })
        return
    }

    if (receiptToView != null) {
        ReceiptDetailScreen(
            receipt = receiptToView!!,
            onBack = { receiptToView = null }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.receipts_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add receipt", tint = IndigoTechnological)
                }
                IconButton(onClick = { showCategories = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Manage categories", tint = getAdaptiveSubtext())
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.receipts_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoTechnological,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberAlert.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = AmberAlert,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isLoading && receipts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = IndigoTechnological)
                }
            } else if (receipts.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = getAdaptiveSubtext()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.receipts_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = getAdaptiveSubtext()
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(receipts, key = { it.id }) { receipt ->
                        ReceiptCard(
                            receipt = receipt,
                            onClick = { receiptToView = receipt },
                            onEdit = { receiptToEdit = receipt },
                            onDelete = { receiptToDelete = receipt }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val prev = currentPage - 1
                            currentPage = prev
                            scope.launch { loadPage(prev) }
                        },
                        enabled = currentPage > 1
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = null)
                        Text(stringResource(R.string.pagination_prev))
                    }
                    Text(
                        text = stringResource(R.string.pagination_page, currentPage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = {
                            val next = currentPage + 1
                            currentPage = next
                            scope.launch { loadPage(next) }
                        },
                        enabled = hasNextPage
                    ) {
                        Text(stringResource(R.string.pagination_next))
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }

    if (receiptToDelete != null) {
        val deleteTitle = stringResource(R.string.receipts_delete_title)
        val deleteMessage = stringResource(R.string.receipts_delete_message, receiptToDelete!!.store)
        val deleteLabel = stringResource(R.string.receipts_delete)
        val cancelLabel = stringResource(R.string.receipts_cancel)
        AlertDialog(
            onDismissRequest = { receiptToDelete = null },
            title = { Text(deleteTitle) },
            text = { Text(deleteMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = receiptToDelete!!.id
                        receiptToDelete = null
                        scope.launch {
                            try {
                                val resp = api.deleteReceipt(id)
                                if (resp.isSuccessful) {
                                    receipts = receipts.filter { it.id != id }
                                } else {
                                    errorMessage = if (resp.code() >= 500) {
                                        context.getString(R.string.auth_unknown_error)
                                    } else {
                                        context.getString(R.string.receipts_delete_error)
                                    }
                                }
                            } catch (e: Exception) {
                                errorMessage = context.getString(R.string.auth_error_internet)
                            }
                        }
                    }
                ) {
                    Text(deleteLabel, color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { receiptToDelete = null }) {
                    Text(cancelLabel)
                }
            },
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface
    )
}

    if (showCreateDialog) {
        CreateReceiptDialog(
            categories = categories,
            onDismiss = { showCreateDialog = false },
            onSave = { request ->
                scope.launch {
                    try {
                        val resp = api.createReceipt(request)
                        if (resp.isSuccessful) {
                            showCreateDialog = false
                            currentPage = 1
                            loadPage(1)
                        } else {
                            errorMessage = context.getString(R.string.receipts_create_error)
                        }
                    } catch (e: Exception) {
                        errorMessage = context.getString(R.string.auth_error_internet)
                    }
                }
            }
        )
    }

    if (receiptToEdit != null) {
        EditReceiptDialog(
            receipt = receiptToEdit!!,
            categories = categories,
            onDismiss = { receiptToEdit = null },
            onSave = { updated ->
                scope.launch {
                    try {
                        val resp = api.updateReceipt(updated.id, ReceiptUpdateRequest(
                            store = updated.store,
                            categoryId = updated.categoryId,
                            total = updated.total,
                            purchaseDate = updated.purchaseDate
                        ))
                        if (resp.isSuccessful) {
                            receipts = receipts.map { if (it.id == updated.id) updated else it }
                            receiptToEdit = null
                        } else {
                            errorMessage = if (resp.code() >= 500) {
                                context.getString(R.string.auth_unknown_error)
                            } else {
                                context.getString(R.string.receipts_update_error)
                            }
                        }
                    } catch (e: Exception) {
                        errorMessage = context.getString(R.string.auth_error_internet)
                    }
                }
            }
        )
    }
}

@Composable
fun ReceiptCard(
    receipt: ReceiptResponse,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        IndigoTechnological.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcons[receipt.categoryIcon] ?: Icons.Default.Store,
                    contentDescription = null,
                    tint = IndigoTechnological,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = receipt.store,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = receipt.categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = IndigoTechnological
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatDate(receipt.purchaseDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = getAdaptiveSubtext()
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = String.format("%.2f€", receipt.total),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = IndigoTechnological,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReceiptDialog(
    receipt: ReceiptResponse,
    categories: List<CategoryResponse>,
    onDismiss: () -> Unit,
    onSave: (ReceiptResponse) -> Unit
) {
    var store by remember { mutableStateOf(receipt.store) }
    var storeError by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableIntStateOf(receipt.categoryId) }
    var total by remember { mutableStateOf(receipt.total.toString()) }
    var totalError by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val selectedCat = categories.find { it.id == selectedCategoryId }
    val selectedCatName = selectedCat?.name ?: ""

    val editTitle = stringResource(R.string.receipts_edit_title)
    val storeLabel = stringResource(R.string.receipts_store_label)
    val storeRequiredMessage = stringResource(R.string.receipts_store_required)
    val categoryLabel = stringResource(R.string.receipts_category_label)
    val totalLabel = stringResource(R.string.receipts_total_label)
    val totalInvalidMessage = stringResource(R.string.receipts_total_invalid)
    val saveLabel = stringResource(R.string.receipts_save)
    val cancelLabel = stringResource(R.string.receipts_cancel)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(editTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = store,
                    onValueChange = { store = it; storeError = false },
                    label = { Text(storeLabel) },
                    singleLine = true,
                    isError = storeError,
                    supportingText = if (storeError) {{ Text(storeRequiredMessage) }} else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoTechnological),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCatName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(categoryLabel) },
                        leadingIcon = {
                            Icon(
                                imageVector = categoryIcons[selectedCat?.icon ?: "Category"] ?: Icons.Default.Category,
                                contentDescription = null,
                                tint = IndigoTechnological
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoTechnological),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = categoryIcons[cat.icon] ?: Icons.Default.Category,
                                            contentDescription = null,
                                            tint = IndigoTechnological,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(cat.name)
                                    }
                                },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = total,
                    onValueChange = {
                        total = it.filter { c -> c.isDigit() || c == ',' || c == '.' }
                        totalError = false
                    },
                    label = { Text(totalLabel) },
                    singleLine = true,
                    isError = totalError,
                    supportingText = if (totalError) {{ Text(totalInvalidMessage) }} else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoTechnological),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalized = total.replace(",", ".")
                    val parsed = normalized.toDoubleOrNull()
                    if (store.isBlank()) { storeError = true; return@TextButton }
                    if (parsed == null || normalized.matches(Regex("^\\d+([.]\\d+)?$")).not() || parsed <= 0) {
                        totalError = true
                        return@TextButton
                    }
                    onSave(
                        receipt.copy(
                            store = store,
                            categoryId = selectedCategoryId,
                            categoryName = selectedCatName,
                            categoryIcon = selectedCat?.icon ?: "Category",
                            total = parsed
                        )
                    )
                }
            ) {
                Text(saveLabel, color = IndigoTechnological)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReceiptDialog(
    categories: List<CategoryResponse>,
    onDismiss: () -> Unit,
    onSave: (ReceiptCreateRequest) -> Unit
) {
    var store by remember { mutableStateOf("") }
    var storeError by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableIntStateOf(categories.firstOrNull()?.id ?: 0) }
    var total by remember { mutableStateOf("") }
    var totalError by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var purchaseDateDisplay by remember { mutableStateOf("") }
    var purchaseDateIso by remember { mutableStateOf("") }

    val selectedCat = categories.find { it.id == selectedCategoryId }
    val selectedCatName = selectedCat?.name ?: ""

    val createTitle = stringResource(R.string.receipts_create_title)
    val storeLabel = stringResource(R.string.receipts_store_label)
    val storeRequiredMessage = stringResource(R.string.receipts_store_required)
    val categoryLabel = stringResource(R.string.receipts_category_label)
    val totalLabel = stringResource(R.string.receipts_total_label)
    val totalInvalidMessage = stringResource(R.string.receipts_total_invalid)
    val dateLabel = stringResource(R.string.receipts_date_label)
    val saveLabel = stringResource(R.string.receipts_save)
    val cancelLabel = stringResource(R.string.receipts_cancel)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(createTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = store,
                    onValueChange = { store = it; storeError = false },
                    label = { Text(storeLabel) },
                    singleLine = true,
                    isError = storeError,
                    supportingText = if (storeError) {{ Text(storeRequiredMessage) }} else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoTechnological),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCatName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(categoryLabel) },
                        leadingIcon = {
                            Icon(
                                imageVector = categoryIcons[selectedCat?.icon ?: "Category"] ?: Icons.Default.Category,
                                contentDescription = null,
                                tint = IndigoTechnological
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoTechnological),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = categoryIcons[cat.icon] ?: Icons.Default.Category,
                                            contentDescription = null,
                                            tint = IndigoTechnological,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(cat.name)
                                    }
                                },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = total,
                    onValueChange = {
                        total = it.filter { c -> c.isDigit() || c == ',' || c == '.' }
                        totalError = false
                    },
                    label = { Text(totalLabel) },
                    singleLine = true,
                    isError = totalError,
                    supportingText = if (totalError) {{ Text(totalInvalidMessage) }} else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoTechnological),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = purchaseDateDisplay,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(dateLabel) },
                    placeholder = { Text("dd/mm/aaaa") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = IndigoTechnological)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoTechnological),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalized = total.replace(",", ".")
                    val parsed = normalized.toDoubleOrNull()
                    if (store.isBlank()) { storeError = true; return@TextButton }
                    if (parsed == null || normalized.matches(Regex("^\\d+([.]\\d+)?$")).not() || parsed <= 0) {
                        totalError = true
                        return@TextButton
                    }
                    val isoDate = if (purchaseDateIso.isNotEmpty()) purchaseDateIso
                    else SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                    onSave(
                        ReceiptCreateRequest(
                            store = store,
                            categoryId = selectedCategoryId,
                            total = parsed,
                            purchaseDate = isoDate
                        )
                    )
                }
            ) {
                Text(saveLabel, color = IndigoTechnological)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = millis
                        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        purchaseDateIso = isoFmt.format(cal.time)
                        purchaseDateDisplay = sdf.format(cal.time)
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = IndigoTechnological)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(cancelLabel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateStr.substringBefore("+").substringBefore("."))
        date?.let { outputFormat.format(it) } ?: dateStr
    } catch (e: Exception) {
        dateStr.take(10)
    }
}