package pt.ipt.dama2026.finscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import kotlinx.coroutines.launch
import pt.ipt.dama2026.finscan.R
import pt.ipt.dama2026.finscan.data.api.ApiClient
import pt.ipt.dama2026.finscan.data.api.models.CategoryCreateRequest
import pt.ipt.dama2026.finscan.data.api.models.CategoryResponse
import pt.ipt.dama2026.finscan.data.api.models.CategoryUpdateRequest
import pt.ipt.dama2026.finscan.data.api.services.CategoryApiService
import pt.ipt.dama2026.finscan.ui.theme.*

private data class ErrorDetail(val detail: String?)

private fun parseErrorBody(errorBody: okhttp3.ResponseBody?): String? {
    if (errorBody == null) return null
    return try {
        val json = errorBody.string()
        Gson().fromJson(json, ErrorDetail::class.java)?.detail
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiClient.getRetrofit().create(CategoryApiService::class.java) }

    var categories by remember { mutableStateOf(listOf<CategoryResponse>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryResponse?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryResponse?>(null) }

    suspend fun loadCategories() {
        isLoading = true
        errorMessage = ""
        try {
            val resp = api.listCategories()
            if (resp.isSuccessful) {
                categories = resp.body() ?: emptyList()
            } else {
                errorMessage = context.getString(R.string.categories_load_error)
            }
        } catch (e: Exception) {
            errorMessage = context.getString(R.string.auth_error_internet)
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadCategories() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.categories_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, tint = IndigoTechnological)
            }
        }

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

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IndigoTechnological)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(categories, key = { it.id }) { cat ->
                    CategoryRow(
                        category = cat,
                        onEdit = { categoryToEdit = cat },
                        onDelete = { categoryToDelete = cat }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CategoryFormDialog(
            title = context.getString(R.string.categories_new),
            initialName = "",
            onDismiss = { showCreateDialog = false },
            onSave = { name ->
                scope.launch {
                    try {
                        val resp = api.createCategory(CategoryCreateRequest(name))
                        if (resp.isSuccessful) {
                            showCreateDialog = false
                            loadCategories()
                        } else {
                            val detail = parseErrorBody(resp.errorBody())
                            errorMessage = if (detail != null) detail else context.getString(R.string.categories_create_error)
                        }
                    } catch (e: Exception) {
                        errorMessage = context.getString(R.string.auth_error_internet)
                    }
                }
            }
        )
    }

    if (categoryToEdit != null) {
        CategoryFormDialog(
            title = context.getString(R.string.categories_edit),
            initialName = categoryToEdit!!.name,
            onDismiss = { categoryToEdit = null },
            onSave = { name ->
                val id = categoryToEdit!!.id
                categoryToEdit = null
                scope.launch {
                    try {
                        val resp = api.updateCategory(id, CategoryUpdateRequest(name))
                        if (resp.isSuccessful) {
                            loadCategories()
                        } else {
                            val detail = parseErrorBody(resp.errorBody())
                            errorMessage = if (detail != null) detail else context.getString(R.string.categories_update_error)
                        }
                    } catch (e: Exception) {
                        errorMessage = context.getString(R.string.auth_error_internet)
                    }
                }
            }
        )
    }

    if (categoryToDelete != null) {
        val deleteTitle = stringResource(R.string.categories_delete_title)
        val deleteMessage = stringResource(R.string.categories_delete_message, categoryToDelete!!.name)
        val deleteLabel = stringResource(R.string.categories_delete)
        val cancelLabel = stringResource(R.string.categories_cancel)
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text(deleteTitle) },
            text = { Text(deleteMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = categoryToDelete!!.id
                        categoryToDelete = null
                        scope.launch {
                            try {
                                val resp = api.deleteCategory(id)
                                if (resp.isSuccessful) {
                                    loadCategories()
                                } else {
                                    val detail = parseErrorBody(resp.errorBody())
                                    errorMessage = if (detail != null) detail else context.getString(R.string.categories_delete_error)
                                }
                            } catch (e: Exception) {
                                errorMessage = context.getString(R.string.auth_error_internet)
                            }
                        }
                    }
                ) { Text(deleteLabel, color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text(cancelLabel) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CategoryRow(
    category: CategoryResponse,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(IndigoTechnological.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Category,
                    contentDescription = null,
                    tint = IndigoTechnological,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = IndigoTechnological, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun CategoryFormDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val nameLabel = stringResource(R.string.categories_name_label)
    val saveLabel = stringResource(R.string.categories_save)
    val cancelLabel = stringResource(R.string.categories_cancel)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(nameLabel) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IndigoTechnological),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim()) }) {
                Text(saveLabel, color = IndigoTechnological)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelLabel) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface
    )
}
