package lt.skautai.android.util

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder

object BarcodeBitmap {
    fun create(content: String, width: Int = 900, height: Int = 260): Bitmap {
        val matrix = MultiFormatWriter().encode(content, BarcodeFormat.CODE_128, width, height)
        return BarcodeEncoder().createBitmap(matrix)
    }
}
