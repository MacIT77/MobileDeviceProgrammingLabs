package com.example.opengllabs.render

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

class Sphere(private val radius: Float = 1.0f, private val color: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)) {

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        attribute vec3 aNormal;
        varying vec3 vNormal;
        varying vec3 vPositionWorld;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
            vNormal = aNormal;
            vPositionWorld = vPosition.xyz;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec3 vNormal;
        varying vec3 vPositionWorld;
        uniform vec4 uColor;
        uniform vec3 uLightPos;
        uniform float uIsEmissive;
        
        void main() {
            if (uIsEmissive > 0.5) {
                gl_FragColor = uColor;
            } else {
                vec3 lightDir = normalize(uLightPos - vPositionWorld);
                vec3 normal = normalize(vNormal);
                float diff = max(dot(normal, lightDir), 0.0);
                vec3 ambient = uColor.rgb * 0.3;
                vec3 diffuse = uColor.rgb * diff * 0.7;
                gl_FragColor = vec4(ambient + diffuse, uColor.a);
            }
        }
    """.trimIndent()

    private var vertexBuffer: FloatBuffer
    private var normalBuffer: FloatBuffer
    private var indexBuffer: ShortBuffer
    private var program: Int
    private var indexCount: Int = 0

    init {
        val (vertices, normals, indices) = generateSphere(16, 16)

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
                position(0)
            }
        }

        normalBuffer = ByteBuffer.allocateDirect(normals.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(normals)
                position(0)
            }
        }

        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(indices)
                position(0)
            }
        }

        indexCount = indices.size

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    private fun generateSphere(latitudeBands: Int, longitudeBands: Int): Triple<FloatArray, FloatArray, ShortArray> {
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
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

                vertices.add(x * radius)
                vertices.add(y * radius)
                vertices.add(z * radius)

                normals.add(x)
                normals.add(y)
                normals.add(z)
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

        return Triple(vertices.toFloatArray(), normals.toFloatArray(), indices.toShortArray())
    }

    fun draw(mvpMatrix: FloatArray, lightPos: FloatArray, isEmissive: Boolean = false) {
        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        val normalHandle = GLES20.glGetAttribLocation(program, "aNormal")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        val lightPosHandle = GLES20.glGetUniformLocation(program, "uLightPos")
        val isEmissiveHandle = GLES20.glGetUniformLocation(program, "uIsEmissive")

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform4fv(colorHandle, 1, color, 0)
        GLES20.glUniform3fv(lightPosHandle, 1, lightPos, 0)
        GLES20.glUniform1f(isEmissiveHandle, if (isEmissive) 1.0f else 0.0f)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, normalBuffer)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}