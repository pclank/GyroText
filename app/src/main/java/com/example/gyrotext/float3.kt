package com.example.gyrotext

import kotlin.math.sqrt

class float3(var x: Float, var y: Float, var z: Float) {

    // Operators
    operator fun unaryMinus(): float3 {return float3(-x, -y, -z)}
    operator fun float3.plusAssign(v: float3)
    {
        x += v.x
        y += v.y
        z += v.z
    }
    operator fun float3.timesAssign(v: Float)
    {
        x *= v
        y *= v
        z *= v
    }
    operator fun float3.divAssign(v: Float)
    {
        x /= v
        y /= v
        z /= v
    }
    operator fun plus(b: float3): float3 {return float3(x + b.x, y + b.y, z + b.z)}
    operator fun minus(b: float3): float3 {return float3(x - b.x, y - b.y, z - b.z)}
    operator fun times(b: float3): float3 {return float3(x * b.x, y * b.y, z * b.z)}
    operator fun times(b: Float): float3 {return float3(x * b, y * b, z * b)}
    operator fun Float.times(b: float3): float3 {return b * this}
    operator fun float3.div(b: Float): float3 {return (1 / b) * this}

    // Related functions
    fun squaredLength(): Float {return x * x + y * y + z * z}
    fun length(): Float {return sqrt(squaredLength())}

    fun randomize()
    {
        x = Math.random().toFloat()
        y = Math.random().toFloat()
        z = Math.random().toFloat()
    }

    fun randomize(min: Float, max: Float)
    {
        x = RandomFloatRange(min, max)
        y = RandomFloatRange(min, max)
        z = RandomFloatRange(min, max)
    }

    fun normalize()
    {
        val inv_len = 1 / length()
        x *= inv_len
        y *= inv_len
        z *= inv_len
    }

    fun dot(b: float3): Float {return x * b.x + y * b.y + z * b.z}

    fun cross(b: float3): float3 {return float3(y * b.z - z * b.y,
        z * b.x - x * b.z,
        x * b.y - y * b.x)}
}

fun randomInUnitSphere(): float3
{
    while (true)
    {
        val p = float3(0.0f, 0.0f, 0.0f)
        p.randomize(-1.0f, 1.0f)
        if (p.squaredLength() < 1)
            return p;
    }
}

fun randomUnitVector(): float3
{
    var res = randomInUnitSphere()
    res.normalize()
    return res
}

fun randomHemisphere(normal: float3): float3
{
    val on_unit_sphere = randomInUnitSphere()
    if (on_unit_sphere.dot(normal) > 0.0f)
        return on_unit_sphere
    else
        return -on_unit_sphere
}

fun reflect(v: float3, n: float3): float3 {return v - (n * 2.0f * v.dot(n))}

// TODO: Add refract function!
