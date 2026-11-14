package com.example.media

import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.example.R
import com.example.data.firebase.FirestoreMediaStore
import com.google.firebase.firestore.FirebaseFirestore
import okio.Path.Companion.toOkioPath

class FsMediaFetcher(
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val context = options.context
        val db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
        val file = FirestoreMediaStore.resolveToCachedFile(context, db, data.toString())
            ?: throw IllegalStateException("Media blob not found: $data")
        val mimeType = FirestoreMediaStore.mimeTypeForExtension(file.extension)
        return SourceResult(
            source = ImageSource(file = file.toOkioPath()),
            mimeType = mimeType,
            dataSource = DataSource.NETWORK
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            return if (data.scheme == FirestoreMediaStore.SCHEME) {
                FsMediaFetcher(data, options)
            } else {
                null
            }
        }
    }
}
