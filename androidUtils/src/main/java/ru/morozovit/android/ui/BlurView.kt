@file:Suppress("DEPRECATION")

package ru.morozovit.android.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.AttributeSet
import android.view.View

class BlurView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var rs: RenderScript? = null
    private var blurScript: ScriptIntrinsicBlur? = null
    private var inputAllocation: Allocation? = null
    private var outputAllocation: Allocation? = null


    var blurRadius = 25f
        set(value) {
            field = value
            invalidate()
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        rs = RenderScript.create(context)
        blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        rs?.destroy()
        rs = null
        blurScript?.destroy()
        blurScript = null
        inputAllocation?.destroy()
        inputAllocation = null
        outputAllocation?.destroy()
        outputAllocation = null
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        // Capture the view's bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // Create Allocation objects for input and output bitmaps
        inputAllocation = Allocation.createFromBitmap(rs, bitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT)
        outputAllocation = Allocation.createTyped(rs, inputAllocation?.type)

        // Set the blur radius
        blurScript?.setRadius(blurRadius)

        // Perform the blur operation
        blurScript?.setInput(inputAllocation)
        blurScript?.forEach(outputAllocation)

        // Copy the blurred bitmap to the canvas
        outputAllocation?.copyTo(bitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // Clean up
        bitmap.recycle()
    }
}