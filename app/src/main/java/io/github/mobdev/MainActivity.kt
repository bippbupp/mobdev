package io.github.mobdev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mobdev.ui.ContactListScreen
import io.github.mobdev.ui.theme.ContactListTheme
import io.github.mobdev.viewmodel.ContactViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContactListTheme {
                val viewModel: ContactViewModel = viewModel()
                ContactListScreen(viewModel = viewModel)
            }
        }
    }
}