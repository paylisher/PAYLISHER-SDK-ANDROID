package com.paylisher.android.notification.helpers

import android.app.Activity
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.paylisher.android.notification.InAppMessagingLayout
import com.paylisher.android.notification.iam.InAppMessagingFullscreen
import com.paylisher.android.notification.iam.InAppMessagingModal

class InAppCarouselModalAdapter(
    private val activity: Activity,
    private val layouts: List<InAppMessagingLayout>,
    private val defaultLang: String?
) : RecyclerView.Adapter<InAppCarouselModalAdapter.CarouselViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        val inAppView = InAppMessagingModal(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return CarouselViewHolder(inAppView)
    }

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        val layout = layouts[position]
        if (defaultLang != null) {
            holder.inAppView.configureLayout(layout, defaultLang, activity)
        }
    }

    override fun getItemCount() = layouts.size

    class CarouselViewHolder(val inAppView: InAppMessagingModal) :
        RecyclerView.ViewHolder(inAppView)
}

class InAppCarouselFullscreenAdapter(
    private val activity: Activity,
    private val layouts: List<InAppMessagingLayout>,
    private val defaultLang: String?
) : RecyclerView.Adapter<InAppCarouselFullscreenAdapter.CarouselViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        val inAppView = InAppMessagingFullscreen(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return CarouselViewHolder(inAppView)
    }

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        val layout = layouts[position]
        if (defaultLang != null) {
            holder.inAppView.configureLayout(layout, defaultLang, activity)
        }
    }

    override fun getItemCount() = layouts.size

    class CarouselViewHolder(val inAppView: InAppMessagingFullscreen) :
        RecyclerView.ViewHolder(inAppView)
}
