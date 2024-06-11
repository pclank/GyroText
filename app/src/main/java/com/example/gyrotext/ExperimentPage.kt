package com.example.gyrotext

data class ExperimentPage(var selectText: String, var tgtText: String, var id: Int, var expType: ExperimentType)
{
    var log: Metrics = Metrics(0.0F)
}
