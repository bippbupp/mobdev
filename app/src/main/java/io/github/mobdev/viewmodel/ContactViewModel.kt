package io.github.mobdev.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.mobdev.data.Contact
import io.github.mobdev.data.fetchAllContacts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ContactViewModel(application: Application) : AndroidViewModel(application) {

    // Список загружается один раз и кешируется — нет повторной загрузки
    private val _contacts = MutableStateFlow<List<Contact>?>(null)
    val contacts: StateFlow<List<Contact>?> = _contacts

    // Выбранный контакт для экрана деталей
    private val _selectedContact = MutableStateFlow<Contact?>(null)
    val selectedContact: StateFlow<Contact?> = _selectedContact

    fun loadContactsIfNeeded() {
        // Загружаем только если ещё не загружали
        if (_contacts.value == null) {
            _contacts.value = getApplication<Application>().fetchAllContacts()
        }
    }

    fun selectContact(contact: Contact) {
        _selectedContact.value = contact
    }

    fun clearSelectedContact() {
        _selectedContact.value = null
    }
}