package com.iris.gallery

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.decode.StaticImageDecoder
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import com.iris.gallery.decoder.AvifCoilDecoder
import com.iris.gallery.data.search.PhotoIndexingWorker

class IrisApplication : Application(), SingletonImageLoader.Factory {
    companion object {
        lateinit var instance: IrisApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        PhotoIndexingWorker.enqueue(this)

        // Force MapLibre into 100% offline mode so ConnectivityReceiver never checks ACCESS_NETWORK_STATE
        try {
            org.maplibre.android.MapLibre.getInstance(this)
            org.maplibre.android.MapLibre.setConnected(false)
            org.maplibre.android.net.ConnectivityReceiver.instance(this).deactivate()
        } catch (_: Exception) {}
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(AvifCoilDecoder.Factory())
                add(coil3.svg.SvgDecoder.Factory())
                if (Build.VERSION.SDK_INT >= 29) {
                    add(AnimatedImageDecoder.Factory())
                    add(StaticImageDecoder.Factory())
                } else if (Build.VERSION.SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}
