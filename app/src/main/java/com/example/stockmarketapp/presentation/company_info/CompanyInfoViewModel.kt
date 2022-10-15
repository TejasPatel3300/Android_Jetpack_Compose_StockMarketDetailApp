package com.example.stockmarketapp.presentation.company_info

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockmarketapp.domain.repository.StockRepository
import com.example.stockmarketapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompanyInfoViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: StockRepository,
) : ViewModel() {

    var state by mutableStateOf(CompanyInfoState())

    init {
        viewModelScope.launch {
            val symbol = savedStateHandle.get<String>("symbol") ?: return@launch
            state = state.copy(isLoading = true)

            val intraDayInfoResult = async { repository.getIntradayInfo(symbol) }
            val companyInfoResult = async { repository.getCompanyInfo(symbol) }

            when (val result = companyInfoResult.await()) {
                is Resource.Error -> {
                    state = state.copy(isLoading = false, error = result.message, company = null)
                }
                is Resource.Success -> {
                    state = state.copy(isLoading = false, error = null, company = result.data)
                }
                else -> Unit
            }

            when (val result = intraDayInfoResult.await()) {
                is Resource.Error -> {
                    state = state.copy(
                        isLoading = false,
                        error = result.message,
                        stockInfos = result.data ?: emptyList()
                    )
                }
                is Resource.Success -> {
                    state = state.copy(
                        isLoading = false,
                        error = null,
                        stockInfos = result.data ?: emptyList()
                    )
                }
                else -> Unit
            }
        }
    }
}