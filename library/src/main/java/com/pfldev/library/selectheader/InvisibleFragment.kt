package com.pfldev.library.selectheader

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


/**
 * Callback for [SelectHeaderBuilder.request] method.
 */
typealias RequestCallback = (path: String) -> Unit

class InvisibleFragment : Fragment() {

    private lateinit var requestCallback: RequestCallback
    private var imageCropFile: File? = null
    private var imageUri: Uri? = null
    private var isCrop = false

    fun requestNow(callback: RequestCallback,  type: Int,crop: Boolean) {
        this.isCrop = crop
        this.requestCallback = callback
        when (type) {
            SelectHeader.TAKE_PHOTO_TAG -> {
                takeCamera()
            }
            SelectHeader.OPEN_ALBUM_TAG -> {
                openAlbum()
            }
        }
    }

    /**
     * 打开系统相册
     */
    private fun openAlbum() {
        //val photoPickerIntent = Intent(Intent.ACTION_GET_CONTENT)
        val photoPickerIntent = Intent(Intent.ACTION_PICK)  //传统打开相册
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, SelectHeader.OPEN_ALBUM_TAG)
    }

    /**
     * 相机拍照
     */
    private fun takeCamera() {
        val intentCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Environment.isExternalStorageLegacy()) {
            imageUri = createImageUri()
            intentCamera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val jpgFile = createImageFile()
            imageUri = FileProvider.getUriForFile(context!!, "${activity!!.packageName}.FileProvider", jpgFile!!)
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intentCamera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            val jpgFile = createImageFile()
            imageUri = Uri.fromFile(jpgFile)
        }
        //将拍照结果保存至 photo_file 的 Uri 中，不保留在相册中
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intentCamera, SelectHeader.TAKE_PHOTO_TAG)
    }

    /**
     * 调用系统裁剪的方法
     */
    private fun startPhoneZoom(sourceUri: Uri?) {
        imageCropFile = createImageFile(true) //创建一个保存裁剪后照片的File
        imageCropFile?.let {
            val intent = Intent("com.android.camera.action.CROP")
            intent.putExtra("crop", "true")
            intent.putExtra("aspectX", 1)    //X方向上的比例
            intent.putExtra("aspectY", 1)    //Y方向上的比例
            intent.putExtra("outputX", 500)  //裁剪区的宽
            intent.putExtra("outputY", 500) //裁剪区的高
            intent.putExtra("scale ", true)  //是否保留比例
            intent.putExtra("return-data", false) //是否在Intent中返回图片
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString()) //设置输出图片的格式

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) //添加这一句表示对目标应用临时授权该Uri所代表的文件
                intent.setDataAndType(sourceUri, "image/*")  //设置数据源,必须是由FileProvider创建的ContentUri

                val imgCropUri = Uri.fromFile(it)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imgCropUri) //设置输出  不需要ContentUri,否则失败
            } else {
                intent.setDataAndType(imageUri, "image/*")
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(it))
            }
            startActivityForResult(intent, PHOTO_CROP)
        }
    }

    /**
     * Android 10 获取 图片 uri
     *
     * @return 生成的图片uri
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    fun createImageUri(): Uri? {
        //设置保存参数到ContentValues中
        val contentValues =  ContentValues()
        //设置文件名
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, createFileName())
        //兼容Android Q和以下版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //android Q中不再使用DATA字段，而用RELATIVE_PATH代替
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/albumCameraImg")
        }
        //设置文件类型
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/JPEG")
        //执行insert操作，向系统文件夹中添加文件
        //EXTERNAL_CONTENT_URI代表外部存储器，该值不变
        return context!!.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }


    /**
     * 生成一个文件
     */
    private fun createImageFile(isCrop: Boolean = false): File? {
        return try {
            val rootFile = File(context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath+File.separator+when(isCrop){
                true ->"albumCameraImgCrop"
                false ->"albumCameraImg"
            })
            if (!rootFile.exists())
                rootFile.mkdirs()
            val fileName = "${createFileName()}.jpg"
            File(rootFile.absolutePath + File.separator + fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun createFileName():String{
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return "IMG_$timeStamp"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SelectHeader.TAKE_PHOTO_TAG && resultCode == RESULT_OK) {
            if(isCrop){
                startPhoneZoom(imageUri)
            }else {
                imageUri?.let {
                    val path = GetImgFromAlbum.getRealPathFromUri(context!!, imageUri!!)
                    requestCallback(path!!)
                }
            }
        } else if (requestCode == SelectHeader.OPEN_ALBUM_TAG && resultCode == RESULT_OK) {
            val uri: Uri? = if (data != null) {
                data.data
            } else {
                return
            }
            if(isCrop){
                startPhoneZoom(uri)
            }else{
                val path = GetImgFromAlbum.getRealPathFromUri(context!!, uri!!)
                requestCallback(path!!)
            }
        } else if (requestCode == PHOTO_CROP && resultCode == RESULT_OK) {
            imageCropFile?.run {
                requestCallback(absolutePath)
            }
        }
    }

    companion object{
        const val PHOTO_CROP = 103
    }

}