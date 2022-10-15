package com.example.stockmarketapp.data.csv

import com.opencsv.CSVReader
import com.example.stockmarketapp.data.mappers.toIntradayInfo
import com.example.stockmarketapp.data.remote.dto.IntradayInfoDto
import com.example.stockmarketapp.domain.model.IntradayInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntraDayParser @Inject constructor(

) : CSVParser<IntradayInfo> {
    override suspend fun parse(stream: InputStream): List<IntradayInfo> {
        val csvReader = CSVReader(InputStreamReader(stream))
        return withContext(Dispatchers.IO) {
            csvReader.readAll()
                .drop(1)
                .mapNotNull { line ->
                    val timestamp = line.getOrNull(0)?: return@mapNotNull null
                    val close = line.getOrNull(1) ?: return@mapNotNull null
                    val dto = IntradayInfoDto(
                        timeStamp = timestamp,
                        close = close.toDouble(),
                    )
                    dto.toIntradayInfo()
                }.filter {
                    it.date.dayOfMonth == LocalDateTime.now().minusDays(3).dayOfMonth
                }
                .sortedBy {
                    it.date.hour
                }
                .also {
                    csvReader.close()
                }
        }
    }
}