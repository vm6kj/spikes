package com.novoda.spikes.arcore

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.widget.Toast
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.novoda.spikes.arcore.google.helper.TapHelper
import com.novoda.spikes.arcore.helper.ARCoreDependenciesHelper
import com.novoda.spikes.arcore.helper.CameraPermissionHelper
import com.novoda.spikes.arcore.poly.PolyAsset
import com.novoda.spikes.arcore.poly.PolyAssetLoader
import com.novoda.spikes.arcore.rendering.NovodaSurfaceViewRenderer
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), MessageDisplayer, ModelsDisplayer, ModelsAdapter.Listener {

    private val modelCollection = ModelCollection(PolyAssetLoader(), this, this)

    private lateinit var arSession: Session
    private lateinit var renderer: NovodaSurfaceViewRenderer
    private lateinit var debugViewDisplayer: DebugViewDisplayer
    private lateinit var tapHelper: TapHelper
    private lateinit var mainThreadHandler: Handler;
    private lateinit var modelsAdapter: ModelsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainThreadHandler = Handler()
        setupSurfaceView()
        modelsAdapter = ModelsAdapter(LayoutInflater.from(this), this)
        modelsListView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        modelsListView.adapter = modelsAdapter
    }

    override fun displayModels(models: List<PolyAsset>) {
        mainThreadHandler.post {
            modelsAdapter.setModels(models)
        }
    }

    override fun onModelClicked(model: PolyAsset) {
        modelCollection.selectModel(model)
    }

    private fun setupSurfaceView() {
        tapHelper = TapHelper(this)
        debugViewDisplayer = DebugViewDisplayer(debugTextView)
        renderer = NovodaSurfaceViewRenderer(this, debugViewDisplayer, modelCollection, tapHelper)

        surfaceView.preserveEGLContextOnPause = true
        surfaceView.setEGLContextClientVersion(2)
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Alpha used for plane blending.
        surfaceView.setRenderer(renderer)
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        surfaceView.setOnTouchListener(tapHelper)
    }

    override fun onResume() {
        super.onResume()
        checkAREnvironment()
    }

    private fun checkAREnvironment() {
        ARCoreDependenciesHelper.isARCoreIsInstalled(this).apply {
            if (isARCoreInstalled && CameraPermissionHelper.isCameraPermissionGranted(this@MainActivity)) {
                createOrResumeARSession()
            } else {
                showMessage(message)
            }
        }
    }

    private fun createOrResumeARSession() {
        if (this::arSession.isInitialized.not()) {
            arSession = Session(this)
            renderer.setSession(arSession)
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            arSession.resume()
        } catch (e: CameraNotAvailableException) {
            // In some cases the camera may be given to a different app instead. Recreate the session at the next iteration.
            showMessage("Camera not available. Please restart the app.")
            return
        }
        surfaceView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        if (this::arSession.isInitialized) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            surfaceView.onPause()
            arSession.pause()
        }
    }

    override fun showMessage(message: String) {
        mainThreadHandler.post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        renderer.close()
        super.onDestroy()
    }

}
