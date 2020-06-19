package com.pfldev.selectheader

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.pfldev.library.selectheader.InvisibleFragment
import com.pfldev.library.selectheader.SelectHeader
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getSysAuthority()
    }

    /**
     * 申请必要权限
     */
    private fun getSysAuthority() {
        AndPermission.with(this).runtime()
            .permission(
                Permission.CAMERA,
                Permission.WRITE_EXTERNAL_STORAGE,
                Permission.READ_EXTERNAL_STORAGE
            )
            .onGranted {

            }.onDenied {

            }.start()
    }

    fun openAlbum(view: View) {
        SelectHeader.init(this)
            .setSelectType(SelectHeader.OPEN_ALBUM_TAG)
            .request {
                Glide.with(this).load(it).into(imgView)
            }
    }

    fun takeCamera(view: View) {
        SelectHeader.init(this)
            .setSelectType(SelectHeader.TAKE_PHOTO_TAG)
            .request {
                Glide.with(this).load(it).into(imgView)
            }
    }
}
