package com.example.vlcdemo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.ToastUtils
import com.example.vlcdemo.databinding.ActivityMainBinding
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IVLCVout

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var libVlc: LibVLC
    private lateinit var mediaPlayer: MediaPlayer

    private val url = "rtsp://10.10.1.163:8080/h264_ulaw.sdp"

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            playVideo(uri)
        }

    private val pickMovieFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val selectedFileUri: Uri? = data?.data

                playVideo(selectedFileUri)
            }
        }

    private val mBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        initVlc()

        mBinding.btnPlay.setOnClickListener {
            val url = mBinding.etUrl.text.toString()
            playRtsp(url)
            KeyboardUtils.hideSoftInput(this)
//            resultLauncher.launch("video/*")
        }
        mBinding.btnPick.setOnClickListener {
            openSAF()
        }
    }

    private fun initVlc() {
        val options = arrayListOf<String>()
        options.add("--rtsp-tcp") // 强制rtsp-tcp，加快加载视频速度
        options.add("--aout=opensles")
        options.add("--audio-time-stretch")
        options.add("-vvv") // 日志级别
        libVlc = LibVLC(this, options)
        mediaPlayer = MediaPlayer(libVlc)
        mediaPlayer.scale = 0f
        mediaPlayer.setAudioOutput("opensles_android")
        mediaPlayer.setEventListener { event: MediaPlayer.Event ->
            when (event.type) {
                MediaPlayer.Event.Buffering -> {
                    mBinding.progressBar.isVisible = event.buffering != 100f
                }

                MediaPlayer.Event.EncounteredError -> {
                    ToastUtils.getDefaultMaker().show("Error")
                }

                MediaPlayer.Event.EndReached -> {
                    ToastUtils.getDefaultMaker().show("End")
                }

                else -> {}
            }
        }
        mediaPlayer.vlcVout.addCallback(object : IVLCVout.Callback {
            override fun onSurfacesCreated(vlcVout: IVLCVout?) {

            }

            override fun onSurfacesDestroyed(vlcVout: IVLCVout?) {

            }
        })
    }

    override fun onStart() {
        super.onStart()
//        mediaPlayer.attachViews(mBinding.videoLayout, null, false, false)
        mediaPlayer.vlcVout.setWindowSize(mBinding.surfaceView.width, mBinding.surfaceView.height)

        mediaPlayer.vlcVout.setVideoView(mBinding.surfaceView)
        mediaPlayer.vlcVout.attachViews()
    }

    override fun onResume() {
        super.onResume()

        if (!mediaPlayer.isPlaying && mediaPlayer.hasMedia()) {
            mediaPlayer.play()
        }
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    override fun onStop() {
        super.onStop()
//        mediaPlayer.detachViews()
        mediaPlayer.vlcVout.detachViews()
    }

    override fun onDestroy() {
        super.onDestroy()

        mediaPlayer.release()
        libVlc.release()
    }

    private fun playVideo(uri: Uri?) {
        if (uri === null) {
            return
        }
        val fd = contentResolver.openFileDescriptor(uri, "r")

        val media = Media(libVlc, fd!!.fileDescriptor)
//        media.setHWDecoderEnabled(true, false)
        media.addOption(":network-caching=600")

        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()
    }

    private fun playRtsp(url: String) {

        val media = Media(libVlc, Uri.parse(url))
//        media.setHWDecoderEnabled(true, false)
        media.addOption(":network-caching=300") // 默认1500
        media.addOption(":clock-jitter=0")
//        media.addOption(":no-audio")
        media.addOption(":clock-synchro=0")

        mediaPlayer.media = media
        media.setHWDecoderEnabled(false, false) // picture is too late to be displayed
        media.release()
        mediaPlayer.play()
    }

    private fun openSAF() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        pickMovieFileLauncher.launch(intent)
    }
}