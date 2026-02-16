package com.example.opengllabs.render

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
    val hasMoon: Boolean = false,
    val moonDistance: Float = 0f,
    val moonSize: Float = 0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PlanetData
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}

class SolarSystem {

    private val sun: Sphere
    private val planets = mutableListOf<Sphere>()
    private val moon: Sphere

    val planetData = listOf(
        PlanetData("Меркурий", 3.0f, 0.2f, floatArrayOf(0.7f, 0.7f, 0.7f, 1f), 2.0f, 1f),
        PlanetData("Венера", 4.0f, 0.35f, floatArrayOf(0.9f, 0.6f, 0.2f, 1f), 1.5f, 0.8f),
        PlanetData("Земля", 5.5f, 0.35f, floatArrayOf(0.2f, 0.5f, 0.9f, 1f), 1.0f, 2f, true, 0.6f, 0.15f),
        PlanetData("Марс", 7.0f, 0.25f, floatArrayOf(0.8f, 0.3f, 0.1f, 1f), 0.8f, 1.8f),
        PlanetData("Юпитер", 10.0f, 0.8f, floatArrayOf(0.8f, 0.7f, 0.5f, 1f), 0.4f, 4f),
        PlanetData("Сатурн", 13.0f, 0.7f, floatArrayOf(0.9f, 0.8f, 0.6f, 1f), 0.3f, 3.5f),
        PlanetData("Уран", 16.0f, 0.5f, floatArrayOf(0.4f, 0.8f, 0.9f, 1f), 0.2f, 2.5f),
        PlanetData("Нептун", 19.0f, 0.48f, floatArrayOf(0.2f, 0.4f, 0.9f, 1f), 0.1f, 2.3f)
    )

    private val orbitAngles = FloatArray(planetData.size) { 0f }
    private val rotationAngles = FloatArray(planetData.size) { 0f }
    private var moonAngle = 0f
    private val moonOrbitTilt = 5.15f

    var selectedPlanetIndex = 2

    init {
        sun = Sphere(0.8f, floatArrayOf(1f, 0.9f, 0.2f, 1f))
        planetData.forEach { data ->
            planets.add(Sphere(data.size, data.color))
        }
        moon = Sphere(0.12f, floatArrayOf(0.8f, 0.8f, 0.8f, 1f))
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

            if (data.hasMoon && index == 2) {
                val moonMatrix = FloatArray(16)
                Matrix.setIdentityM(moonMatrix, 0)
                Matrix.translateM(moonMatrix, 0, x, 0f, z)
                Matrix.rotateM(moonMatrix, 0, moonOrbitTilt, 1f, 0f, 0f)

                val moonRad = Math.toRadians(moonAngle.toDouble())
                val mx = (data.moonDistance * cos(moonRad)).toFloat()
                val mz = (data.moonDistance * sin(moonRad)).toFloat()
                Matrix.translateM(moonMatrix, 0, mx, 0f, mz)

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
        return "${data.name}\nРасстояние: ${data.distance} а.е.\nРазмер: ${data.size}"
    }

    fun getSelectedPlanetData(): PlanetData = planetData[selectedPlanetIndex]

    fun getOrbitAngle(index: Int): Float = orbitAngles[index]
}