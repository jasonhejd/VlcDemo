package com.example.vlcdemo

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.KeyboardUtils
import com.example.vlcdemo.databinding.ActivityMainBinding
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IVLCVout
import org.videolan.libvlc.util.VLCVideoLayout

class MainActivity : AppCompatActivity()
{
    private lateinit var libVlc: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var videoLayout: VLCVideoLayout

    private val url = "rtsp://10.10.1.163:8080/h264_ulaw.sdp"

    private val resultLauncher = registerForActivityResult(GetContent()) { uri: Uri? ->
        playVideo(uri)
    }

    private val mBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        val options = arrayListOf<String>()
        options.add("--rtsp-tcp") // 强制rtsp-tcp，加快加载视频速度
        options.add("--aout=opensles")
        options.add("--audio-time-stretch")
        options.add("-vvv") // 日志级别
        libVlc = LibVLC(this, options)
        mediaPlayer = MediaPlayer(libVlc)
        mediaPlayer.setAudioOutput("opensles_android")
        mediaPlayer.setEventListener { event: MediaPlayer.Event ->
            when (event.type) {
                MediaPlayer.Event.Buffering -> {
                    Toast.makeText(this, "Buffering", Toast.LENGTH_SHORT).show()
                }
                MediaPlayer.Event.EncounteredError -> {
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
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
        videoLayout = findViewById(R.id.videoLayout)

        val button: Button = findViewById(R.id.btn_play)
        button.setOnClickListener {
            val url = mBinding.etUrl.text.toString()
            playRtsp(url)
            KeyboardUtils.hideSoftInput(this)
//            resultLauncher.launch("video/*")
        }
    }

    override fun onStart() {
        super.onStart()
        mediaPlayer.attachViews(videoLayout, null, false, false)
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
            mediaPlayer.stop()
        }
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.detachViews()
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
        media.setHWDecoderEnabled(true, false)
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
}