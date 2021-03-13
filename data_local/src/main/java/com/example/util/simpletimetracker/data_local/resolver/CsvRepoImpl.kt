package com.example.util.simpletimetracker.data_local.resolver

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.util.simpletimetracker.domain.model.Category
import com.example.util.simpletimetracker.domain.model.Record
import com.example.util.simpletimetracker.domain.model.RecordType
import com.example.util.simpletimetracker.domain.repo.RecordRepo
import com.example.util.simpletimetracker.domain.repo.RecordTypeCategoryRepo
import com.example.util.simpletimetracker.domain.repo.RecordTypeRepo
import com.example.util.simpletimetracker.domain.resolver.CsvRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class CsvRepoImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val recordTypeRepo: RecordTypeRepo,
    private val recordRepo: RecordRepo,
    private val recordTypeCategoryRepo: RecordTypeCategoryRepo
) : CsvRepo {

    override suspend fun saveCsvFile(uriString: String): CsvRepo.ResultCode = withContext(Dispatchers.IO) {
        var fileDescriptor: ParcelFileDescriptor? = null
        var fileOutputStream: FileOutputStream? = null

        try {
            val uri = Uri.parse(uriString)
            fileDescriptor = contentResolver.openFileDescriptor(uri, "w")
            fileOutputStream = fileDescriptor?.fileDescriptor?.let(::FileOutputStream)

            // Write csv header
            val csvHeaderRow: String = CSV_HEADER + "\n"
            fileOutputStream?.write(csvHeaderRow.toByteArray())

            val recordTypes = recordTypeRepo.getAll()
                .map { it.id to it }.toMap()

            // Write data
            recordRepo.getAll()
                .sortedBy { it.timeStarted }
                .forEach { record ->
                    val categories = recordTypeCategoryRepo.getCategoriesByType(record.typeId)

                    toCsvString(record, recordTypes[record.typeId], categories)
                        ?.toByteArray()
                        ?.let { fileOutputStream?.write(it) }
                }

            fileOutputStream?.close()
            fileDescriptor?.close()
            CsvRepo.ResultCode.SUCCESS
        } catch (e: Exception) {
            Timber.e(e)
            CsvRepo.ResultCode.ERROR
        } finally {
            try {
                fileOutputStream?.close()
                fileDescriptor?.close()
            } catch (e: IOException) {
                // Do nothing
            }
        }
    }

    private fun toCsvString(record: Record, recordType: RecordType?, tags: List<Category>): String? {
        return if (recordType != null) {
            String.format(
                "%s,%s,%s,%s,%s\n",
                recordType.name.clean(),
                formatDateTime(record.timeStarted),
                formatDateTime(record.timeEnded),
                record.comment.clean(),
                tags.takeUnless { it.isEmpty() }?.joinToString(separator = " ") { it.name.clean() }.orEmpty()
            )
        } else {
            null
        }
    }

    private fun formatDateTime(timestamp: Long): String {
        synchronized(dateTimeFormat) {
            return dateTimeFormat.format(timestamp)
        }
    }

    private fun String.clean() =
        replace("[\n\t]".toRegex(), " ").replace(",", " ")

    companion object {
        private const val CSV_HEADER = "activity name,time started,time ended,comment,tags"

        private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }
}