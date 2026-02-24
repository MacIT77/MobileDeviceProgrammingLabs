package com.example.opengllabs.render

import android.content.Context
import android.opengl.GLES20
import com.example.opengllabs.R
import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.sin

data class PlanetData(
    val name: String,
    val distance: Float,
    val size: Float,
    val color: FloatArray,
    val orbitSpeed: Float,
    val rotationSpeed: Float,
    val textureName: String,
    val hasMoon: Boolean = false,
    val moonDistance: Float = 0f,
    val moonSize: Float = 0f,
    val moonTextureName: String = "moon",
    val hasRings: Boolean = false,
    val ringTextureName: String? = null,
    val ringInnerRadius: Float = 1.7f,
    val ringOuterRadius: Float = 3.2f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PlanetData
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}

class SolarSystem(private val context: Context) {

    private val sun: Sphere
    private val planets = mutableListOf<Sphere>()
    private val moon: Sphere
    private var saturnRings: SaturnRings? = null

    private val textureMap = mapOf(
        "sun" to R.drawable.sun,
        "mercury" to R.drawable.mercury,
        "venus" to R.drawable.venus,
        "earth" to R.drawable.earth,
        "mars" to R.drawable.mars,
        "jupiter" to R.drawable.jupiter,
        "saturn" to R.drawable.saturn,
        "uranus" to R.drawable.uranus,
        "neptune" to R.drawable.neptune,
        "moon" to R.drawable.moon,
        "saturn_rings" to R.drawable.saturn_rings
    )

    val planetData = listOf(
        PlanetData("Меркурий", 3.0f, 0.2f, floatArrayOf(0.7f, 0.7f, 0.7f, 1f), 2.0f, 1f, "mercury"),
        PlanetData("Венера", 4.0f, 0.35f, floatArrayOf(0.9f, 0.6f, 0.2f, 1f), 1.5f, 0.8f, "venus"),
        PlanetData("Земля", 5.5f, 0.35f, floatArrayOf(0.2f, 0.5f, 0.9f, 1f), 1.0f, 2f, "earth", true, 0.6f, 0.15f, "moon"),
        PlanetData("Марс", 7.0f, 0.25f, floatArrayOf(0.8f, 0.3f, 0.1f, 1f), 0.8f, 1.8f, "mars"),
        PlanetData("Юпитер", 10.0f, 0.8f, floatArrayOf(0.8f, 0.7f, 0.5f, 1f), 0.4f, 4f, "jupiter"),
        PlanetData("Сатурн", 13.0f, 0.7f, floatArrayOf(0.9f, 0.8f, 0.6f, 1f), 0.3f, 3.5f, "saturn",
            hasRings = true, ringTextureName = "saturn_rings", ringInnerRadius = 1.7f, ringOuterRadius = 3.2f),
        PlanetData("Уран", 16.0f, 0.5f, floatArrayOf(0.4f, 0.8f, 0.9f, 1f), 0.2f, 2.5f, "uranus"),
        PlanetData("Нептун", 19.0f, 0.48f, floatArrayOf(0.2f, 0.4f, 0.9f, 1f), 0.1f, 2.3f, "neptune")
    )

    private val orbitAngles = FloatArray(planetData.size) { 0f }
    private val rotationAngles = FloatArray(planetData.size) { 0f }
    private var moonAngle = 0f
    private val moonOrbitTilt = 5.15f
    private val saturnRingTilt = 26.7f

    var selectedPlanetIndex = 2

    init {
        sun = Sphere(
            context = context,
            radius = 0.8f,
            color = floatArrayOf(1f, 0.9f, 0.2f, 1f),
            textureResId = textureMap["sun"]
        )

        planetData.forEach { data ->
            planets.add(
                Sphere(
                    context = context,
                    radius = data.size,
                    color = data.color,
                    textureResId = textureMap[data.textureName]
                )
            )

            if (data.hasRings) {
                saturnRings = SaturnRings(
                    context = context,
                    innerRadius = data.ringInnerRadius,
                    outerRadius = data.ringOuterRadius,
                    textureResId = data.ringTextureName?.let { textureMap[it] }
                )
            }
        }

        moon = Sphere(
            context = context,
            radius = 0.12f,
            color = floatArrayOf(0.8f, 0.8f, 0.8f, 1f),
            textureResId = textureMap["moon"]
        )
    }

    fun update(deltaTime: Float) {
        planetData.forEachIndexed { index, data ->
            orbitAngles[index] += data.orbitSpeed * deltaTime * 10f
            rotationAngles[index] += data.rotationSpeed * deltaTime * 20f
        }
        moonAngle += 15f * deltaTime
    }

    fun draw(viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        val mvpMatrix = FloatArray(16)
        val modelMatrix = FloatArray(16)
        val tempMatrix = FloatArray(16)

        val lightPos = floatArrayOf(0f, 0f, 0f)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
        sun.draw(mvpMatrix, lightPos, true)

        planetData.forEachIndexed { index, data ->
            Matrix.setIdentityM(modelMatrix, 0)

            val rad = Math.toRadians(orbitAngles[index].toDouble())
            val x = (data.distance * cos(rad)).toFloat()
            val z = (data.distance * sin(rad)).toFloat()

            Matrix.translateM(modelMatrix, 0, x, 0f, z)
            Matrix.rotateM(modelMatrix, 0, rotationAngles[index], 0f, 1f, 0f)

            Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

            planets[index].draw(mvpMatrix, lightPos, false)

            if (data.hasRings) {
                val ringMatrix = FloatArray(16)
                Matrix.setIdentityM(ringMatrix, 0)
                Matrix.translateM(ringMatrix, 0, x, 0f, z)
                Matrix.rotateM(ringMatrix, 0, saturnRingTilt, 1f, 0f, 0f)

                Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, ringMatrix, 0)
                Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

                GLES20.glDepthMask(false)
                saturnRings?.draw(mvpMatrix, saturnRingTilt)
                GLES20.glDepthMask(true)
            }

            if (data.hasMoon && index == 2) {
                val moonMatrix = FloatArray(16)
                Matrix.setIdentityM(moonMatrix, 0)
                Matrix.translateM(moonMatrix, 0, x, 0f, z)

                val earthAngleRad = Math.toRadians(orbitAngles[index].toDouble())
                val perpX = (-sin(earthAngleRad)).toFloat()
                val perpZ = (cos(earthAngleRad)).toFloat()

                val moonRad = Math.toRadians(moonAngle.toDouble())
                val mx = (data.moonDistance * cos(moonRad) * perpX).toFloat()
                val my = (data.moonDistance * sin(moonRad)).toFloat()
                val mz = (data.moonDistance * cos(moonRad) * perpZ).toFloat()
                Matrix.translateM(moonMatrix, 0, mx, my, mz)

                Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, moonMatrix, 0)
                Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

                moon.draw(mvpMatrix, lightPos, false)
            }
        }
    }

    fun selectNextPlanet() {
        selectedPlanetIndex = (selectedPlanetIndex + 1) % planetData.size
    }

    fun selectPreviousPlanet() {
        selectedPlanetIndex = if (selectedPlanetIndex > 0) selectedPlanetIndex - 1 else planetData.size - 1
    }

    fun getSelectedPlanetInfo(): String {
        val data = planetData[selectedPlanetIndex]
        val ringsInfo = if (data.hasRings) " (с кольцами)" else ""
        return "${data.name}$ringsInfo\nРасстояние: ${data.distance} а.е.\nРазмер: ${data.size}"
    }

    fun getSelectedPlanetData(): PlanetData = planetData[selectedPlanetIndex]

    fun getOrbitAngle(index: Int): Float = orbitAngles[index]
}