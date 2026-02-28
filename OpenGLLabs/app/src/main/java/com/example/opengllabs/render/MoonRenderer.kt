package com.example.opengllabs.render

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.opengllabs.R
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class MoonRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private lateinit var moon: Sphere

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    private var time = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        moon = Sphere(
            context = context,
            radius = 1.0f, // Larger for close-up view
            color = floatArrayOf(0.8f, 0.8f, 0.8f, 1f),
            textureResId = R.drawable.moon
        )
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        time += 0.016f

        // Gentle camera orbit around the Moon for dynamic lighting view
        val eyeX = (cos(time * 0.2) * 4f).toFloat()
        val eyeZ = (sin(time * 0.2) * 4f).toFloat()
        Matrix.setLookAtM(viewMatrix, 0, eyeX, 0f, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f)

        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, time * 10f, 0f, 1f, 0f) // Slow rotation

        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        val mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        val lightPos = floatArrayOf(5f, 5f, 5f) // Light from upper-right
        val viewPos = floatArrayOf(eyeX, 0f, eyeZ) // Current camera pos

        moon.draw(mvpMatrix, lightPos, isEmissive = false, viewPos = viewPos, useSpecular = true) // Enable Phong
    }
}