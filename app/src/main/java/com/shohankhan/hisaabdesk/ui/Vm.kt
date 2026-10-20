package com.shohankhan.hisaabdesk.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shohankhan.hisaabdesk.HisaabDeskApplication

/** Convenience factory so screens can build ViewModels from the app container. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(noinline creator: (HisaabDeskApplication) -> VM): VM {
    val factory = remember {
        viewModelFactory {
            initializer {
                val app = this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HisaabDeskApplication
                creator(app)
            }
        }
    }
    return viewModel(factory = factory)
}
