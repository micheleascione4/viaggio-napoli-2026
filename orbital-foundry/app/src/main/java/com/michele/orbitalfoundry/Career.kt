package com.michele.orbitalfoundry

data class CareerState(
    val funds:Int = 100000,
    val science:Int = 0,
    val reputation:Int = 0,
    val unlocked:Set<String> = setOf("starter_engine","small_tank","capsule"),
    val completedContracts:Set<String> = emptySet()
)

data class Contract(
    val id:String,
    val title:String,
    val description:String,
    val reward:Int,
    val science:Int,
    val required:String? = null
)

object CareerDatabase {
    val contracts = listOf(
        Contract("suborbital","First Light","Reach 80 km altitude and return safely.",15000,5),
        Contract("orbit","Orbital Qualification","Complete one stable Earth orbit.",30000,12,"suborbital"),
        Contract("satellite","Tiny Satellite","Deploy a probe into a 120 km circular orbit.",45000,18,"orbit"),
        Contract("recovery","Safe Recovery","Land the vehicle safely after a flight.",20000,8,"suborbital"),
        Contract("moon","Lunar Pathfinder","Reach lunar orbit.",90000,35,"satellite"),
        Contract("landing","Lunar Landing","Land on the Moon and transmit science.",150000,60,"moon"),
        Contract("mars","Red Planet","Reach Mars transfer trajectory.",300000,100,"landing")
    )

    val tech = listOf(
        Tech("starter_engine","Starter Engine",0,0,null),
        Tech("small_tank","Small Fuel Tank",0,0,"starter_engine"),
        Tech("capsule","Crew Capsule",0,0,"starter_engine"),
        Tech("fairing","Payload Fairing",25000,10,"orbit"),
        Tech("rcs","RCS Thrusters",40000,15,"satellite"),
        Tech("landing","Landing Legs",60000,20,"moon"),
        Tech("ion","Ion Engine",100000,35,"satellite"),
        Tech("heavy","Heavy Lift Engine",150000,45,"moon"),
        Tech("solar","Deployable Solar",80000,25,"satellite")
    )
}

data class Tech(
    val id:String,
    val title:String,
    val cost:Int,
    val science:Int,
    val prerequisite:String?
)

fun canAccept(contract:Contract,state:CareerState):Boolean =
    contract.required == null || contract.required in state.completedContracts

fun completeContract(contract:Contract,state:CareerState):CareerState =
    state.copy(
        funds=state.funds+contract.reward,
        science=state.science+contract.science,
        reputation=state.reputation+1,
        completedContracts=state.completedContracts+contract.id
    )

fun unlockTech(tech:Tech,state:CareerState):CareerState? {
    if(tech.id in state.unlocked) return state
    if(tech.prerequisite!=null && tech.prerequisite !in state.unlocked) return null
    if(state.funds<tech.cost || state.science<tech.science) return null
    return state.copy(
        funds=state.funds-tech.cost,
        science=state.science-tech.science,
        unlocked=state.unlocked+tech.id
    )
}
