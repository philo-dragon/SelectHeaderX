package com.pfldev.library.selectheader

import androidx.fragment.app.FragmentActivity

class SelectHeaderBuilder internal constructor(private val activity: FragmentActivity) {

    private var TAG = SelectHeaderBuilder::class.java.simpleName
    private var type = SelectHeader.TAKE_PHOTO_TAG
    private var isCrop = true

    fun setSelectType(type: Int): SelectHeaderBuilder {
        this.type = type
        return this
    }

    fun setCrop(isCrop: Boolean): SelectHeaderBuilder {
        this.isCrop = isCrop
        return this
    }

    fun request(callback: RequestCallback) {
        getInvisibleFragment().requestNow(callback, type ,isCrop)
    }

    /**
     * Get the invisible fragment in activity for request permissions.
     * If there is no invisible fragment, add one into activity.
     * Don't worry. This is very lightweight.
     */
    private fun getInvisibleFragment(): InvisibleFragment {
        val fragmentManager = activity.supportFragmentManager
        val existedFragment = fragmentManager.findFragmentByTag(TAG)
        return if (existedFragment != null) {
            existedFragment as InvisibleFragment
        } else {
            val invisibleFragment = InvisibleFragment()
            fragmentManager.beginTransaction().add(invisibleFragment, TAG).commitNow()
            invisibleFragment
        }
    }


}