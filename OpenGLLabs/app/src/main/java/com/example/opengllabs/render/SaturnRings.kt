package com.example.opengllabs.render

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

class SaturnRings(
    private val context: Context,
    private val innerRadius: Float = 1.2f,
    private val outerRadius: Float = 2.2f,
    private val textureResId: Int? = null
) {

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        varying float vDistance;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
            vTexCoord = aTexCoord;
            vDistance = length(vPosition.xz);
        }
    """.trimIndent()

    private val fragmentShaderCode = """
    precision mediump float;
    varying vec2 vTexCoord;
    varying float vDistance;
    uniform sampler2D uTexture;
    uniform float uUseTexture;
    
    void main() {
        vec4 finalColor;
        
        float innerR = ${innerRadius};
        float outerR = ${outerRadius};

        float t = (vDistance - innerR) / (outerR - innerR);
        
        if (uUseTexture > 0.5) {
            finalColor = texture2D(uTexture, vTexCoord);
        } else {
            float bands = sin(t * 50.0) * 0.5 + 0.5;
            float fineBands = sin(t * 200.0) * 0.3 + 0.7;
            float pattern = bands * fineBands;

            vec3 ringColor = mix(
                vec3(0.7, 0.6, 0.4),
                vec3(0.9, 0.85, 0.7),
                pattern
            );

            float alpha = 1.0;
            if (t < 0.05) alpha = t / 0.05;
            if (t > 0.95) alpha = (1.0 - t) / 0.05;
            
            finalColor = vec4(ringColor, alpha * 0.9);
        }

        if (finalColor.a < 0.01) discard;
        
        gl_FragColor = finalColor;
    }
""".trimIndent()

    private var vertexBuffer: FloatBuffer
    private var texCoordBuffer: FloatBuffer
    private var indexBuffer: ShortBuffer
    private var program: Int
    private var indexCount: Int = 0
    private var textureId: Int = 0

    init {
        val ringData = generateRings(64)

        vertexBuffer = ByteBuffer.allocateDirect(ringData.vertices.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(ringData.vertices)
                position(0)
            }
        }

        texCoordBuffer = ByteBuffer.allocateDirect(ringData.texCoords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(ringData.texCoords)
                position(0)
            }
        }

        indexBuffer = ByteBuffer.allocateDirect(ringData.indices.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(ringData.indices)
                position(0)
            }
        }

        indexCount = ringData.indices.size

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        textureResId?.let {
            textureId = loadTexture(it)
        }
    }

    private data class RingData(
        val vertices: FloatArray,
        val texCoords: FloatArray,
        val indices: ShortArray
    )

    private fun generateRings(segments: Int): RingData {
        val vertices = mutableListOf<Float>()
        val texCoords = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        val radii = listOf(innerRadius, outerRadius)

        for (i in 0..segments) {
            val angle = i * 2f * Math.PI / segments
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()

            for (r in radii) {
                vertices.add(r * cosA)
                vertices.add(0f)
                vertices.add(r * sinA)

                val u = i / segments.toFloat()
                val v = if (r == innerRadius) 0f else 1f
                texCoords.add(u)
                texCoords.add(v)
            }
        }

        for (i in 0 until segments) {
            val base = i * 2

            indices.add(base.toShort())
            indices.add((base + 1).toShort())
            indices.add((base + 2).toShort())

            indices.add((base + 1).toShort())
            indices.add((base + 3).toShort())
            indices.add((base + 2).toShort())
        }

        return RingData(
            vertices.toFloatArray(),
            texCoords.toFloatArray(),
            indices.toShortArray()
        )
    }

    private fun loadTexture(resourceId: Int): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        val textureId = textureIds[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)

        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return textureId
    }

    fun draw(mvpMatrix: FloatArray, tiltAngle: Float = 26.7f) {
        GLES20.glUseProgram(program)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        val useTextureHandle = GLES20.glGetUniformLocation(program, "uUseTexture")

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(useTextureHandle, if (textureResId != null) 1.0f else 0.0f)

        if (textureResId != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glUniform1i(textureHandle, 0)
        }

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)

        if (textureResId != null) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }

        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}