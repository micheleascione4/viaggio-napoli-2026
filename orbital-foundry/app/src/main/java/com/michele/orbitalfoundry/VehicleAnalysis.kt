package com.michele.orbitalfoundry

data class VehicleAnalysis(
    val totalMass:Double,
    val dryMass:Double,
    val fuelMass:Double,
    val thrust:Double,
    val twr:Double,
    val deltaV:Double,
    val estimatedMaxAltitudeKm:Int,
    val warnings:List<String>
)

fun analyzeVehicle(rocket:Rocket):VehicleAnalysis{
    val mass=rocket.dryMass+rocket.fuel
    val twr=if(mass>0)rocket.thrust/(mass*9.81) else 0.0
    val dv=rocket.deltaV*1000.0
    val altitude=(dv*dv/(2*9.80665)/1000.0).toInt()
    val warnings=buildList{
        if(!rocket.hasEngine)add("No engine installed")
        if(!rocket.hasCapsule)add("No command capsule")
        if(twr<1.05)add("TWR below safe liftoff threshold")
        if(rocket.fuel<=0)add("No usable propellant")
        if(rocket.parts.size>18)add("Vehicle is getting heavy and complex")
    }
    return VehicleAnalysis(mass,rocket.dryMass,rocket.fuel,rocket.thrust,twr,dv,altitude,warnings)
}
