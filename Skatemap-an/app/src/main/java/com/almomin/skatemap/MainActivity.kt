package com.almomin.skatemap

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStorageDirectory
import android.provider.MediaStore
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.GeolocationPermissions
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var myWebView: WebView
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null
    private val REQUEST_CAMERA_PERMISSION_CODE = 1
    private lateinit var cameraImageUri : Uri

    // private var mFilePathCallback: ValueCallback<Array<Uri>>? = null

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = filesDir

        Log.d("MYWEB", "storageDir = ${storageDir}")
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",  /* suffix */
                storageDir /* directory */
        )
    }

    private val fileUploadActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                Log.d("MYWEB", "fileUploadActivityResultLauncher(rst code) ${result.resultCode}")
                val intent = result.data
                // val action = intent?.action
                var results: Array<Uri>? = emptyArray()


                Log.e("MYWEB", "result is ${Activity.RESULT_OK}")
                if (result.resultCode == Activity.RESULT_OK) {

                    //var results: Array<Uri>? = null

                    if (intent?.data != null || intent?.clipData != null) { // file picker case
                        // File picker case
                        Log.d("MYWEB", "FILE Picker case")


                        val clipData = intent.clipData

                        if (clipData != null) {
                            Log.d("MYWEB", "clipdata is not null and only one picking")
                            Log.d("MYWEB", "clipData = $clipData and numbers ${clipData.itemCount}")
                            for (i in 0 until clipData.itemCount) {
                                val item = clipData.getItemAt(i)
                                if (item != null) {
                                    Log.d("MYWEB", "index $i is not null and uri = ${item.uri}")
                                    results = results?.plus(item.uri)
                                    Log.d(
                                            "MYWEB",
                                            "results = ${
                                                results?.get(0).toString()
                                            } and length ${results?.size}"
                                    )
                                } else {
                                    Log.d("MYWEB", "index $i item is null")
                                }

                            }

                        } else {
                            Log.d("MYWEB", "clipdata is null and only one picking")
                            results = result.data?.let {
                                WebChromeClient.FileChooserParams.parseResult(
                                        result.resultCode,
                                        it
                                )
                            }
                        }


                        // Print the Uri parsed result
                        Log.d(
                                "MYWEB",
                                "results = ${results?.get(0).toString()} and length ${results?.size}"
                        )

                        fileUploadCallback?.onReceiveValue(results)
                        fileUploadCallback = null

                    } else {
                        //Camera case
                        Log.d("MYWEB", "CAMERA case")

                        results = arrayOf(mCameraPhotoPath!!.toUri())


                        Log.d(
                                "MYWEB",
                                "results = ${results.get(0)} and length ${results.size}"
                        )

                        fileUploadCallback?.onReceiveValue(results)
                        fileUploadCallback = null

                    }


                } else {
                    Log.d("MYWEB", "Cancel case, do Nothing")
                    fileUploadCallback?.onReceiveValue(results)
                    fileUploadCallback = null
                }

                Log.d("MYWEB", "Action: ${intent?.action}")
                Log.d("MYWEB", "Data: ${intent?.data}")
                Log.d("MYWEB", "Categories: ${intent?.categories}")
                Log.d("MYWEB", "Flags: ${intent?.flags}")
                Log.d("MYWEB", "Component: ${intent?.component}")
                Log.d("MYWEB", "Extra: ${intent?.extras}")
                Log.d("MYWEB", "ClipData: ${intent?.clipData}")

                return@registerForActivityResult

            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)







        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d("MYWEB", "Above R case")
            window.setDecorFitsSystemWindows(false)
            val controller = window.insetsController
            if (controller != null) {
                //controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        } else {
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }


        //request location permission
        if ((ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED)
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    )
            ) {
                ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.CAMERA),
                        1
                )

                ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                        1
                )
            } else {
                ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.CAMERA),
                        1
                )
                ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                        1
                )
            }
        }

        myWebView = findViewById(R.id.webview)

        myWebView.webViewClient = MyWebViewClient()
        //myWebView.setWebViewClient(MyWebViewClient())
        myWebView.getSettings().textZoom = 100
        val webSettings = myWebView.getSettings()
        webSettings.javaScriptEnabled = true


        myWebView.settings.allowFileAccess = true
        myWebView.settings.domStorageEnabled = true
        myWebView.settings.allowContentAccess = true

        // Enable file upload from the WebView
        myWebView.webChromeClient = object : WebChromeClient() {



            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                onJsAlert(message!!, result!!)
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                onJsConfirm(message!!, result!!)
                return true
            }

            override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback
            ) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
                callback.invoke(origin, true, false)
            }

            override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
            ): Boolean {
                Log.d("MYWEB", "onShowFileChooser")
                fileUploadCallback = filePathCallback ?: return false


                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                if (takePictureIntent?.let {
                            packageManager.queryIntentActivities(
                                    it,
                                    PackageManager.MATCH_DEFAULT_ONLY
                            )
                        } != null) {

                    // Create the File where the photo should go
                    Log.d("MYWEB", "takePictureIntent resolved " + takePictureIntent.toString())
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)

                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        Log.e("ErrorCreatingFile", "Unable to create Image File", ex)
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.absolutePath
                        Log.d("MYWEB", "Photo file creation $mCameraPhotoPath")
                        if( Build.VERSION.SDK_INT > Build.VERSION_CODES.N){
                            val strpa = applicationContext.packageName
                            cameraImageUri  = FileProvider.getUriForFile(this@MainActivity,
                                    strpa + ".fileprovider", photoFile
                            )
                        } else {
                            cameraImageUri = Uri.fromFile((photoFile))
                        }

                        takePictureIntent.putExtra(
                                MediaStore.EXTRA_OUTPUT,
                                cameraImageUri
                        )
                    } else {
                        Log.d("MYWEB", "photoFile null case")
                        takePictureIntent = null
                    }
                } else {
                    Log.d("MYWEB", "Intent resolving error")
                }

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                /// 여러개의 이미지를 선택하게 해준다.
                contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                contentSelectionIntent.type = "image/*"

                val intentArray: Array<Intent?>
                intentArray = if (takePictureIntent != null) {
                    arrayOf(takePictureIntent)
                } else {
                    arrayOfNulls(0)
                }


                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)

                //fileUploadActivityResultLauncher.launch(intent)
                //startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)
                fileUploadActivityResultLauncher.launch(/* input = */ chooserIntent)
                return true
            }

        }

        /// make status bar and navigation bar transparent
        /// getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)


        myWebView.loadUrl("https://oldjeans.io/skatemap2.0.html")
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
                this, arrayOf<String>(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                            ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }

            0 -> {
                if (grantResults[0] == 0)
                    Toast.makeText(this, "카메라 권한이 승인됨", Toast.LENGTH_SHORT).show()
                else
                    Toast.makeText(this, "카메라 권한이 거절됨", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class MyWebViewClient : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if ("oldjeans.io" == Uri.parse(url).host) {
                // This is my website, so do not override; let my WebView load the page
                Log.d("MYWEB", "skatemap host case")
                return false
            }

            if (url.startsWith("https://firebasestorage.googleapis.com")) {
                // Handle the request yourself, potentially fetching and displaying the resource manually
                // (This approach involves complex logic and security considerations beyond this scope)
                Log.d("MYWEB", "firebase case")
                return true; // Indicate that you've handled the URL
            }


            // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            return true
        }

        override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
        ): WebResourceResponse? {
            // Handle downloads from the WebView
            return super.shouldInterceptRequest(view, request)
        }
    }

    override fun onBackPressed() {
        if (myWebView.canGoBack()) {
            myWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    fun onJsAlert(message : String, result : JsResult) : Unit{
        var builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("알림")
        builder.setMessage(message)
        builder.setIcon(R.mipmap.ic_launcher20)

        // 버튼 클릭 이벤트
        var listener = DialogInterface.OnClickListener { _, clickEvent ->
            when (clickEvent) {
                DialogInterface.BUTTON_POSITIVE ->{
                    result!!.confirm()
                }
            }
        }
        builder.setPositiveButton(android.R.string.ok, listener)
        builder.show()
    }

    fun onJsConfirm(message : String, result : JsResult) : Unit {
        var builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("알림")
        builder.setMessage(message)
        builder.setIcon(R.mipmap.ic_launcher20)

        // 버튼 클릭 이벤트
        var listener = DialogInterface.OnClickListener { _, clickEvent ->
            when (clickEvent) {
                DialogInterface.BUTTON_POSITIVE ->{
                    result!!.confirm()
                }

                DialogInterface.BUTTON_NEUTRAL -> {
                    result!!.cancel()
                }
            }
        }
        builder.setPositiveButton(android.R.string.ok, listener)
        builder.setNeutralButton(android.R.string.cancel, listener)
        builder.show()
    }
}

