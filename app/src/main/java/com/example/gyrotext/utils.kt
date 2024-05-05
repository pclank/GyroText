package com.example.gyrotext

fun RandomFloatRange(min: Float, max: Float): Float
{
    return min + (max - min) * Math.random().toFloat()
}