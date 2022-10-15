package com.example.stockmarketapp.data.repository

import android.util.Log
import com.example.stockmarketapp.data.csv.CSVParser
import com.example.stockmarketapp.data.local.StockDatabase
import com.example.stockmarketapp.data.mappers.toCompanyInfo
import com.example.stockmarketapp.data.mappers.toCompanyListing
import com.example.stockmarketapp.data.mappers.toCompanyListingEntity
import com.example.stockmarketapp.data.remote.StockApi
import com.example.stockmarketapp.domain.model.CompanyInfo
import com.example.stockmarketapp.domain.model.CompanyListing
import com.example.stockmarketapp.domain.model.IntradayInfo
import com.example.stockmarketapp.domain.repository.StockRepository
import com.example.stockmarketapp.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val api: StockApi,
    private val db: StockDatabase,
    private val companyListingParser: CSVParser<CompanyListing>,
    private val intraDayInfoParser: CSVParser<IntradayInfo>
) : StockRepository {
    private val dao = db.stockDao

    override suspend fun getCompanyListings(
        fetchFromRemote: Boolean,
        query: String
    ): Flow<Resource<List<CompanyListing>>> {
        return flow {
            emit(Resource.Loading(Loading = true))
            val localListings = dao.searchCompanyListing(query)
            emit(Resource.Success(data = localListings.map { it.toCompanyListing() }))

            val dbIsEmpty = localListings.isEmpty() && query.isBlank()
            val shouldLoadFromCache = !dbIsEmpty && !fetchFromRemote

            if (shouldLoadFromCache) {
                emit(Resource.Loading(false))
                return@flow
            }

            val remoteListing = try {
                val response = api.getListing()
                companyListingParser.parse(response.byteStream())
            } catch (e: IOException) {
                e.printStackTrace()
                emit(Resource.Error(null, message = "Couldn't load data"))
                null
            } catch (e: HttpException) {
                e.printStackTrace()
                emit(Resource.Error(null, message = "Couldn't load data"))
                null
            }

            remoteListing?.let { listing ->
                dao.clearCompanyListings()
                dao.insertCompanyListings(listing.map { it.toCompanyListingEntity() })

                emit(Resource.Success(
                    data = dao.searchCompanyListing("")
                        .map { it.toCompanyListing() }
                ))
                emit(Resource.Loading(false))
            }
        }
    }

    override suspend fun getIntradayInfo(symbol: String): Resource<List<IntradayInfo>> {
       return  try {
            val response = api.getIntradayInfo(symbol)
            val result = intraDayInfoParser.parse(response.byteStream())
            Resource.Success(result)
        }catch (e: IOException){
            Log.e("", e.toString())
            Resource.Error(message = "Could not get data!", data = null)
        }
        catch (e:HttpException){
            Log.e("", e.toString())
            Resource.Error(message = "Could not get data!", data = null)
        }
    }

    override suspend fun getCompanyInfo(symbol: String): Resource<CompanyInfo> {
        return try {
            val response = api.getCompanyInfo(symbol)
            Resource.Success(response.toCompanyInfo())
        }catch (e: IOException){
            Log.e("", e.toString())
            Resource.Error(message = "Could not get data!", data = null)
        }
        catch (e:HttpException){
            Log.e("", e.toString())
            Resource.Error(message = "Could not get data!", data = null)
        }
    }
}