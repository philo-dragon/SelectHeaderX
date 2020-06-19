package com.pfldev.library.selectheader

import androidx.fragment.app.FragmentActivity

object SelectHeader {

    const val TAKE_PHOTO_TAG = 101

    const val OPEN_ALBUM_TAG = 102

    fun init(activity: FragmentActivity) = SelectHeaderBuilder(activity)
}