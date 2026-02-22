package com.example.opengllabs.render

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import com.example.opengllabs.R
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class MyGLRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private lateinit var square: Square
    private lateinit var solarSystem: SolarSystem
    private lateinit var selectionCube: WireframeCube

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    private var textureId: Int = 0
    private var time = 0f

    var onPlanetSelected: ((String) -> Unit)? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.05f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        square = Square()
        solarSystem = SolarSystem(context)
        selectionCube = WireframeCube()

        textureId = loadTexture(R.drawable.galaxy)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setLookAtM(viewMatrix, 0, 0f, 12f, -20f, 0f, 0f, 0f, 0f, 1f, 0f)

        solarSystem.update(0.008f)
        time += 0.008f

        drawBackground()

        solarSystem.draw(viewMatrix, projectionMatrix)

        drawSelectionCube()
    }

    private fun drawBackground() {
        val mvpMatrix = FloatArray(16)
        val modelMatrix = FloatArray(16)
        val tempMatrix = FloatArray(16)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0f, 5f)
        Matrix.scaleM(modelMatrix, 0, 30f, 30f, 1f)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        square.draw(mvpMatrix, textureId)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    private fun drawSelectionCube() {
        val mvpMatrix = FloatArray(16)
        val modelMatrix = FloatArray(16)
        val tempMatrix = FloatArray(16)

        val data = solarSystem.getSelectedPlanetData()
        val index = solarSystem.selectedPlanetIndex

        val rad = Math.toRadians(solarSystem.getOrbitAngle(index).toDouble())
        val x = (data.distance * cos(rad)).toFloat()
        val z = (data.distance * sin(rad)).toFloat()

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, 0f, z)

        val scale = data.size * 2.5f + 0.1f * kotlin.math.sin(time * 3f)
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale)

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        val alpha = 0.4f + 0.2f * kotlin.math.sin(time * 4f)
        selectionCube.draw(mvpMatrix, alpha)
    }

    fun selectNextPlanet() {
        solarSystem.selectNextPlanet()
        onPlanetSelected?.invoke(solarSystem.getSelectedPlanetInfo())
    }

    fun selectPreviousPlanet() {
        solarSystem.selectPreviousPlanet()
        onPlanetSelected?.invoke(solarSystem.getSelectedPlanetInfo())
    }

    fun getSelectedPlanetInfo(): String = solarSystem.getSelectedPlanetInfo()

    private fun loadTexture(resourceId: Int): Int {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)

        if (textureHandle[0] != 0) {
            val options = BitmapFactory.Options()
            options.inScaled = false
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            bitmap.recycle()
        }

        return textureHandle[0]
    }
}