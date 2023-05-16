package io.rami.barcodekeyboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import androidx.core.content.ContextCompat
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView

class BarcodeInputService : InputMethodService(), ZXingScannerView.ResultHandler {
    var scannerView: ZXingScannerView? = null
    var button: Button? = null
    var lastText: String = ""
    var lastTime: Long = 0
    override fun onCreateInputView(): View {
        val v = layoutInflater.inflate(R.layout.input, null)
        scannerView = v.findViewById(R.id.zxing_scanner)

        button = v.findViewById<Button>(R.id.button)
        button!!.setOnClickListener {
            val i = Intent(this, PermissionCheckActivity::class.java)
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }
        enforcePermission()
        return v
    }

    fun enforcePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val i = Intent(this, PermissionCheckActivity::class.java)
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        } else {
            button?.visibility = View.GONE
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        scannerView?.setResultHandler(this)
        enforcePermission()
        scannerView?.startCamera()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        scannerView?.stopCamera()
    }
    
    private fun extractCodeFromText(text: String): String {
        val pattern = Pattern.compile("component/([A-Z0-9-]+)")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return ""
    }

    override fun handleResult(rawResult: Result) {
        val extractedCode = extractCodeFromText(rawResult.text)
        scannerView?.resumeCameraPreview(this)
        if (extractedCode == lastText && System.currentTimeMillis() - lastTime < 5000) {
            return
        }
        lastText = extractedCode
        lastTime = System.currentTimeMillis()
        currentInputConnection.also { ic: InputConnection ->
            ic.commitText(extractedCode, 1)
        }
    }
}
