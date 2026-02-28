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

class Sphere(
    private val context: Context,
    private val radius: Float = 1.0f,
    private val color: FloatArray = floatArrayOf(1f, 1f, 1f, 1f),
    private val textureResId: Int? = null
) {

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        attribute vec3 aNormal;
        attribute vec2 aTexCoord;
        varying vec3 vNormal;
        varying vec3 vPositionWorld;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
            vNormal = aNormal;
            vPositionWorld = vPosition.xyz;
            vTexCoord = aTexCoord;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec3 vNormal;
        varying vec3 vPositionWorld;
        varying vec2 vTexCoord;
        uniform vec4 uColor;
        uniform vec3 uLightPos;
        uniform vec3 uViewPos;
        uniform float uIsEmissive;
        uniform float uUseSpecular;
        uniform sampler2D uTexture;
        uniform float uUseTexture;
        
        void main() {
            vec4 finalColor;
            
            if (uUseTexture > 0.5) {
                finalColor = texture2D(uTexture, vTexCoord);
            } else {
                finalColor = uColor;
            }
            
            if (uIsEmissive > 0.5) {
                gl_FragColor = finalColor;
            } else {
                vec3 lightDir = normalize(uLightPos - vPositionWorld);
                vec3 normal = normalize(vNormal);
                float diff = max(dot(normal, lightDir), 0.0);
                vec3 ambient = finalColor.rgb * 0.3;
                vec3 diffuse = finalColor.rgb * diff * 0.7;
                gl_FragColor = vec4(ambient + diffuse, finalColor.a);
            }
        }
    """.trimIndent()

    private var vertexBuffer: FloatBuffer
    private var normalBuffer: FloatBuffer
    private var texCoordBuffer: FloatBuffer
    private var indexBuffer: ShortBuffer
    private var program: Int
    private var indexCount: Int = 0
    private var textureId: Int = 0

    init {
        val sphereData = generateSphere(32, 32)

        vertexBuffer = ByteBuffer.allocateDirect(sphereData.vertices.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(sphereData.vertices)
                position(0)
            }
        }

        normalBuffer = ByteBuffer.allocateDirect(sphereData.normals.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(sphereData.normals)
                position(0)
            }
        }

        texCoordBuffer = ByteBuffer.allocateDirect(sphereData.texCoords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(sphereData.texCoords)
                position(0)
            }
        }

        indexBuffer = ByteBuffer.allocateDirect(sphereData.indices.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(sphereData.indices)
                position(0)
            }
        }

        indexCount = sphereData.indices.size

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        // Загружаем текстуру если есть
        textureResId?.let {
            textureId = loadTexture(it)
        }
    }

    private data class SphereData(
        val vertices: FloatArray,
        val normals: FloatArray,
        val texCoords: FloatArray,
        val indices: ShortArray
    )

    private fun generateSphere(latitudeBands: Int, longitudeBands: Int): SphereData {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val texCoords = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        for (lat in 0..latitudeBands) {
            val theta = lat * Math.PI / latitudeBands
            val sinTheta = sin(theta)
            val cosTheta = cos(theta)

            for (long in 0..longitudeBands) {
                val phi = long * 2 * Math.PI / longitudeBands
                val sinPhi = sin(phi)
                val cosPhi = cos(phi)

                val x = (cosPhi * sinTheta).toFloat()
                val y = cosTheta.toFloat()
                val z = (sinPhi * sinTheta).toFloat()

                // Вершина
                vertices.add(x * radius)
                vertices.add(y * radius)
                vertices.add(z * radius)

                // Нормаль
                normals.add(x)
                normals.add(y)
                normals.add(z)

                // Текстурные координаты (u, v)
                val u = 1f - (long / longitudeBands.toFloat())
                val v = 1f - (lat / latitudeBands.toFloat())
                texCoords.add(u)
                texCoords.add(v)
            }
        }

        for (lat in 0 until latitudeBands) {
            for (long in 0 until longitudeBands) {
                val first = (lat * (longitudeBands + 1) + long).toShort()
                val second = (first + longitudeBands + 1).toShort()

                indices.add(first)
                indices.add(second)
                indices.add((first + 1).toShort())

                indices.add(second)
                indices.add((second + 1).toShort())
                indices.add((first + 1).toShort())
            }
        }

        return SphereData(
            vertices.toFloatArray(),
            normals.toFloatArray(),
            texCoords.toFloatArray(),
            indices.toShortArray()
        )
    }

    private fun loadTexture(resourceId: Int): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        val textureId = textureIds[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // Параметры фильтрации
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        // Параметры оборачивания
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)

        // Загружаем bitmap
        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)

        bitmap.recycle()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        return textureId
    }

    fun draw(mvpMatrix: FloatArray, lightPos: FloatArray, isEmissive: Boolean = false, viewPos: FloatArray = floatArrayOf(0f, 0f, 0f), useSpecular: Boolean = false) {
        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        val normalHandle = GLES20.glGetAttribLocation(program, "aNormal")
        val texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        val lightPosHandle = GLES20.glGetUniformLocation(program, "uLightPos")
        val viewPosHandle = GLES20.glGetUniformLocation(program, "uViewPos")
        val isEmissiveHandle = GLES20.glGetUniformLocation(program, "uIsEmissive")
        val useSpecularHandle = GLES20.glGetAttribLocation(program, "uUseSpecular")
        val textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        val useTextureHandle = GLES20.glGetUniformLocation(program, "uUseTexture")

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform4fv(colorHandle, 1, color, 0)
        GLES20.glUniform3fv(lightPosHandle, 1, lightPos, 0)
        GLES20.glUniform1f(isEmissiveHandle, if (isEmissive) 1.0f else 0.0f)
        GLES20.glUniform1f(useTextureHandle, if (textureResId != null) 1.0f else 0.0f)
        GLES20.glUniform3fv(viewPosHandle, 1, viewPos, 0)
        GLES20.glUniform1f(useSpecularHandle, if(useSpecular) 1.0f else 0.0f)

        // Привязываем текстуру
        if (textureResId != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glUniform1i(textureHandle, 0)
        }

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, normalBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)

        // Отвязываем текстуру
        if (textureResId != null) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}