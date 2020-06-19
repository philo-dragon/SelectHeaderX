package com.pfldev.library.selectheader

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File

class GetImgFromAlbum {
    companion object {

        /**
         * 根据Uri获取图片的绝对路径
         *
         * @param context 上下文对象
         * @param uri     图片的Uri
         * @return 如果Uri对应的图片存在, 那么返回该图片的绝对路径, 否则返回null
         */
        @SuppressLint("ObsoleteSdkInt")
        fun getRealPathFromUri(context: Context, uri: Uri): String? {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    getRealPathFromUriAboveApiAndroidQ(context, uri)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    getRealPathFromUriAboveApiAndroidK(context, uri)
                }
                else -> {
                    getRealPathFromUriBelowApiAndroidK(context, uri)
                }
            }
        }

        /**
         * 适配 Android 10以上相册选取照片操作
         *
         * @param context 上下文
         * @param uri     图片uri
         * @return 图片地址
         */
        private fun getRealPathFromUriAboveApiAndroidQ(context: Context, uri: Uri): String? {
            var cursor: Cursor? = null
            val path = getRealPathFromUriAboveApiAndroidK(context, uri)
            try {
                cursor = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    , arrayOf(MediaStore.Images.Media._ID)
                    , MediaStore.Images.Media.RELATIVE_PATH + "=? ",
                    arrayOf(path),
                    null
                )
                return if (cursor != null && cursor.moveToFirst()) {
                    val id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
                    val baseUri =Uri.parse("content://media/external/images/media")
                    Uri.withAppendedPath(baseUri, "" + id).toString()
                } else {
                    // 如果图片不在手机的共享图片数据库，就先把它插入。
                    if (File(path).exists()) {
                        val values = ContentValues()
                        values.put(MediaStore.Images.Media.DATA, path)
                        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).toString()
                    } else {
                        null
                    }
                }
            } catch (e: java.lang.Exception) {
                cursor?.close()
            }
            return null
        }

        /**
         * 适配Android 4.4以下(不包括api19),根据uri获取图片的绝对路径
         *
         * @param context 上下文对象
         * @param uri     图片的Uri
         * @return 如果Uri对应的图片存在, 那么返回该图片的绝对路径, 否则返回null
         */
        private fun getRealPathFromUriBelowApiAndroidK(
            context: Context,
            uri: Uri
        ): String? {
            return getDataColumn(context, uri, null, null)
        }

        /**
         * 适配Android 4.4及以上,根据uri获取图片的绝对路径
         *
         * @param context 上下文对象
         * @param uri     图片的Uri
         * @return 如果Uri对应的图片存在, 那么返回该图片的绝对路径, 否则返回null
         */
        @SuppressLint("NewApi")
        private fun getRealPathFromUriAboveApiAndroidK(context: Context,uri: Uri): String? {
            var filePath: String? = null
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // 如果是document类型的 uri, 则通过document id来进行处理
                val documentId = DocumentsContract.getDocumentId(uri)
                if (isMediaDocument(uri)) {
                    // 使用':'分割
                    val id = documentId.split(":").toTypedArray()[1]
                    val selection = MediaStore.Images.Media._ID + "=?"
                    val selectionArgs = arrayOf(id)
                    filePath = getDataColumn(
                        context,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        selection,
                        selectionArgs
                    )
                } else if (isDownloadsDocument(uri)) {
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(documentId)
                    )
                    filePath = getDataColumn(context, contentUri, null, null)
                }
            } else if ("content".equals(uri.scheme, ignoreCase = true)) {
                // 如果是 content 类型的 Uri
                filePath = getDataColumn(context, uri, null, null)
            } else if ("file" == uri.scheme) {
                // 如果是 file 类型的 Uri,直接获取图片对应的路径
                filePath = uri.path
            }
            return filePath
        }

        /**
         * 获取数据库表中的 _data 列，即返回Uri对应的文件路径
         */
        private fun getDataColumn(context: Context, uri: Uri,selection: String?, selectionArgs: Array<String>?): String? {
            var path: String? = null
            val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(MediaStore.Images.Media.RELATIVE_PATH)
            } else {
                arrayOf(MediaStore.Images.Media.DATA)
            }
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(projection[0])
                    path = cursor.getString(columnIndex)
                }
            } catch (e: Exception) {
                cursor?.close()
            }
            return path
        }

        /**
         * @param uri the Uri to check
         * @return Whether the Uri authority is MediaProvider
         */
        private fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

        /**
         * @param uri the Uri to check
         * @return Whether the Uri authority is DownloadsProvider
         */
        private fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }
    }
}