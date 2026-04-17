package io.github.mobdev.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.mobdev.R
import io.github.mobdev.data.Contact
import io.github.mobdev.viewmodel.ContactViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(viewModel: ContactViewModel) {
    val context = LocalContext.current

    // Состояние разрешения
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Лаунчер для запроса разрешения
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    val contacts by viewModel.contacts.collectAsState()
    val selectedContact by viewModel.selectedContact.collectAsState()

    // Загружаем контакты при наличии разрешения (только один раз благодаря ViewModel)
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.loadContactsIfNeeded()
        }
    }

    // Если выбран контакт — показываем экран деталей
    if (selectedContact != null) {
        ContactDetailScreen(
            contact = selectedContact!!,
            onBack = { viewModel.clearSelectedContact() }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.contacts_list_title)) })
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                !hasPermission -> {
                    // Нет разрешения — показываем соответствующий экран
                    NoPermissionScreen(
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    )
                }
                contacts == null -> {
                    // Загрузка
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    // Показываем список через LazyColumn (не держит всех детей сразу)
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(contacts!!) { contact ->
                            ContactItem(
                                contact = contact,
                                onClick = { viewModel.selectContact(contact) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoPermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.permission_required_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.permission_required_body),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text(stringResource(R.string.request_permission_button))
        }
    }
}

@Composable
fun ContactItem(contact: Contact, onClick: () -> Unit) {
    val name = contact.name ?: stringResource(R.string.contact_no_name)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyLarge)
    }
    HorizontalDivider()
}