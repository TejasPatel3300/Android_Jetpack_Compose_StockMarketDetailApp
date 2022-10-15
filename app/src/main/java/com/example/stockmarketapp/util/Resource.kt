package com.example.stockmarketapp.util

sealed class Resource<T> (val data: T? = null, val message:String? = null){
    class Success<T>(data: T?):Resource<T>(data)
    class Error<T>(data: T?, message: String):Resource<T>(data,message)
    class Loading<T>(val Loading:Boolean = true):Resource<T>(null)
}