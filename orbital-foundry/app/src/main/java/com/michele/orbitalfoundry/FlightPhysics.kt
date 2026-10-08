package com.michele.orbitalfoundry

import kotlin.math.*

/*
 * Orbital Foundry flight model.
 * Position: km from Earth's centre. Velocity: km/s. Mass: tonnes.
 * Thrust: kN. Time: seconds. This keeps orbital mechanics and displayed units consistent.
 */
const val EARTH_RADIUS_KM = 6371.0
const val EARTH_MU = 398600.4418
const val MOON_MU = 4902.8001
const val MOON_ORBIT_KM = 384400.0
const val G0 = 9.80665

data class V2(val x: Double, val y: Double) {
    operator fun plus(o: V2) = V2(x + o.x, y + o.y)
    operator fun minus(o: V2) = V2(x - o.x, y - o.y)
    operator fun times(k: Double) = V2(x * k, y * k)
    operator fun div(k: Double) = V2(x / k, y / k)
    fun mag() = sqrt(x * x + y * y)
    fun normalized(): V2 = if (mag() > 1e-12) this / mag() else V2(0.0, 0.0)
    fun dot(o: V2) = x * o.x + y * o.y
}

data class Orbital(
    val apoapsisAltitudeKm: Double,
    val periapsisAltitudeKm: Double,
    val eccentricity: Double,
    val periodSeconds: Double,
    val escape: Boolean
)

fun orbitalMetrics(pos: V2, vel: V2): Orbital {
    val radius = pos.mag().coerceAtLeast(1.0)
    val energy = vel.dot(vel) / 2.0 - EARTH_MU / radius
    val h = abs(pos.x * vel.y - pos.y * vel.x)
    val e2 = 1.0 + 2.0 * energy * h * h / (EARTH_MU * EARTH_MU)
    val eccentricity = sqrt(e2.coerceAtLeast(0.0))
    if (energy >= 0.0) {
        val peri = (h * h / EARTH_MU / (1.0 + eccentricity) - EARTH_RADIUS_KM)
            .coerceAtLeast(-EARTH_RADIUS_KM)
        return Orbital(1_000_000.0, peri, eccentricity, Double.POSITIVE_INFINITY, true)
    }
    val semiMajor = -EARTH_MU / (2.0 * energy)
    val peri = semiMajor * (1.0 - eccentricity)
    val apo = semiMajor * (1.0 + eccentricity)
    val period = 2.0 * Math.PI * sqrt(semiMajor.pow(3) / EARTH_MU)
    return Orbital(apo - EARTH_RADIUS_KM, peri - EARTH_RADIUS_KM, eccentricity, period, false)
}

/** Parts are stored top-to-bottom in the editor; a decoupler divides the upper stack from the lower stage. */
fun activeStageParts(parts: List<PartType>): List<PartType> {
    val separator = parts.indexOfLast { it == PartType.DECOUPLER }
    return if (separator < 0) parts else parts.drop(separator + 1)
}

fun separateStage(state: SimState): SimState {
    if (state.docked || state.parts.none { it == PartType.DECOUPLER }) return state.copy(throttle = 0.0)
    val separator = state.parts.indexOfLast { it == PartType.DECOUPLER }
    val remaining = state.parts.take(separator)
    val active = activeStageParts(remaining)
    if (remaining.isEmpty() || active.isEmpty()) {
        return state.copy(parts = remaining, fuel = 0.0, mass = 0.1, thrust = 0.0, throttle = 0.0, stage = state.stage + 1)
    }
    return state.copy(
        parts = remaining,
        fuel = active.sumOf { it.fuel },
        mass = remaining.sumOf { it.mass } + remaining.sumOf { it.fuel },
        thrust = active.sumOf { it.thrust },
        throttle = 0.0,
        stage = state.stage + 1,
        trail = state.trail.takeLast(180)
    )
}

fun beginDockingPractice(rocket: Rocket): SimState {
    val radius = EARTH_RADIUS_KM + 200.0
    val position = V2(radius, 0.0)
    val velocity = V2(0.0, sqrt(EARTH_MU / radius))
    val active = activeStageParts(rocket.parts)
    val tangent = V2(0.0, 1.0)
    return SimState(
        pos = position,
        vel = velocity,
        angle = Math.PI / 2.0,
        throttle = 0.0,
        fuel = active.sumOf { it.fuel },
        mass = rocket.dryMass + rocket.fuel,
        thrust = active.sumOf { it.thrust },
        parts = rocket.parts,
        time = 0.0,
        stage = 1,
        guidance = Guidance.MANUAL,
        targetPos = position + tangent * 0.040,
        targetVel = velocity + tangent * 0.00015,
        docking = true,
        trail = listOf(position)
    )
}

fun applyDockingApproach(state: SimState): SimState {
    val target = state.targetPos ?: return state
    val targetVelocity = state.targetVel ?: return state
    val lineOfSight = (target - state.pos).normalized()
    // A discrete RCS pulse: 0.5 m/s towards the target.
    val newVelocity = targetVelocity + lineOfSight * 0.0005
    return state.copy(vel = newVelocity, throttle = 0.0)
}

fun matchDockingVelocity(state: SimState): SimState {
    val targetVelocity = state.targetVel ?: return state
    return state.copy(vel = targetVelocity, throttle = 0.0)
}

fun attemptDock(state: SimState): SimState {
    val target = state.targetPos ?: return state
    val targetVelocity = state.targetVel ?: return state
    val rangeMetres = (target - state.pos).mag() * 1000.0
    val relativeSpeed = (targetVelocity - state.vel).mag() * 1000.0
    return if (rangeMetres <= 5.0 && relativeSpeed <= 0.5) {
        state.copy(docked = true, docking = true, vel = targetVelocity, pos = target, throttle = 0.0)
    } else state
}

private fun moonPosition(timeSeconds: Double): V2 {
    val period = 27.321661 * 86400.0
    val angle = 2.0 * Math.PI * timeSeconds / period
    return V2(cos(angle) * MOON_ORBIT_KM, sin(angle) * MOON_ORBIT_KM)
}

private fun gravityAcceleration(position: V2, timeSeconds: Double): V2 {
    val r = position.mag().coerceAtLeast(1.0)
    var acceleration = position * (-EARTH_MU / r.pow(3))
    val moon = moonPosition(timeSeconds)
    val relative = position - moon
    val distance = relative.mag().coerceAtLeast(100.0)
    // Lunar gravity becomes significant on transfer trajectories; negligible in low Earth orbit.
    acceleration += relative * (-MOON_MU / distance.pow(3))
    return acceleration
}

private fun atmosphericDensity(altitudeKm: Double): Double {
    if (altitudeKm >= 180.0 || altitudeKm < 0.0) return 0.0
    return 1.225 * exp(-altitudeKm / 8.5)
}

private fun thrustAndIsp(parts: List<PartType>): Pair<Double, Double> {
    val engines = parts.filter { it.thrust > 0.0 }
    val thrust = engines.sumOf { it.thrust }
    val isp = if (thrust > 0.0) engines.sumOf { it.thrust * it.ispSec } / thrust else 0.0
    return thrust to isp
}

fun stepPhysics(input: SimState, dtSeconds: Double, pitchDegrees: Double): SimState {
    if (input.crashed || input.landed || input.docked) return input
    val steps = ceil(dtSeconds.coerceIn(0.001, 4.0) / 0.20).toInt().coerceAtLeast(1)
    val dt = dtSeconds / steps
    var s = input
    repeat(steps) {
        val altitude = s.pos.mag() - EARTH_RADIUS_KM
        val stageParts = activeStageParts(s.parts)
        val (stageThrust, isp) = thrustAndIsp(stageParts)
        val actualThrottle = if (s.fuel > 1e-7 && isp > 0.0) s.throttle else 0.0
        val speedKmS = s.vel.mag()
        val speedMS = speedKmS * 1000.0
        val density = atmosphericDensity(altitude)
        val dragArea = if (s.parachuteDeployed) 95.0 else {
            1.8 + stageParts.count { it == PartType.FIN } * 0.65 +
                stageParts.count { it == PartType.LANDING_LEGS } * 0.35
        }
        val dragForceN = 0.5 * density * speedMS * speedMS * dragArea * 0.52
        val dragAcceleration = if (speedKmS > 1e-8 && s.mass > 1e-5) {
            s.vel.normalized() * (-dragForceN / (s.mass * 1_000_000.0))
        } else V2(0.0, 0.0)

        val targetAngle = when (s.guidance) {
            Guidance.PROGRADE -> if (speedKmS > 0.03) atan2(s.vel.y, s.vel.x) else s.angle
            Guidance.RETROGRADE -> if (speedKmS > 0.03) atan2(-s.vel.y, -s.vel.x) else s.angle
            Guidance.HOLD_ORBIT -> if (speedKmS > 0.03) atan2(s.vel.y, s.vel.x) else s.angle
            Guidance.MANUAL -> Math.toRadians(pitchDegrees)
        }
        val thrustAcceleration = if (actualThrottle > 0.0 && s.mass > 1e-5) {
            V2(cos(targetAngle), sin(targetAngle)) * (stageThrust * actualThrottle / s.mass / 1000.0)
        } else V2(0.0, 0.0)
        val acceleration = gravityAcceleration(s.pos, s.time) + dragAcceleration + thrustAcceleration
        val newVelocity = s.vel + acceleration * dt
        val newPosition = s.pos + newVelocity * dt
        val flowTonnesPerSecond = if (actualThrottle > 0.0 && isp > 0.0) stageThrust * actualThrottle / (isp * G0) else 0.0
        val burned = min(s.fuel, flowTonnesPerSecond * dt).coerceAtLeast(0.0)
        val newFuel = (s.fuel - burned).coerceAtLeast(0.0)
        val newMass = (s.mass - burned).coerceAtLeast(0.1)
        val newAltitude = newPosition.mag() - EARTH_RADIUS_KM
        val newSpeedMS = newVelocity.mag() * 1000.0
        val rho = atmosphericDensity(newAltitude)
        val qKpa = 0.5 * rho * newSpeedMS * newSpeedMS / 1000.0
        val heating = sqrt((rho / 1.225).coerceAtLeast(0.0)) *
            (newSpeedMS / 7800.0).pow(3) * if (PartType.HEATSHIELD in s.parts) 0.003 else 0.012
        val heat = (s.heat + heating * dt).coerceIn(0.0, 1.5)
        val groundSpeedMS = newVelocity.mag() * 1000.0
        val hitGround = newAltitude <= 0.0
        val landed = hitGround && groundSpeedMS <= if (s.parachuteDeployed) 18.0 else 4.0
        val crashed = hitGround && !landed
        var targetPosition = s.targetPos
        var targetVelocity = s.targetVel
        if (s.docking && targetPosition != null && targetVelocity != null) {
            val targetA = gravityAcceleration(targetPosition, s.time)
            targetVelocity = targetVelocity + targetA * dt
            targetPosition = targetPosition + targetVelocity * dt
        }
        s = s.copy(
            pos = if (landed) newPosition.normalized() * EARTH_RADIUS_KM else newPosition,
            vel = newVelocity,
            angle = targetAngle,
            fuel = newFuel,
            mass = newMass,
            thrust = stageThrust,
            time = s.time + dt,
            crashed = crashed,
            landed = landed,
            heat = heat,
            maxDynamicPressureKpa = max(s.maxDynamicPressureKpa, qKpa),
            targetPos = targetPosition,
            targetVel = targetVelocity,
            trail = if (it % 2 == 0) (s.trail + newPosition).takeLast(720) else s.trail
        )
        if (s.crashed || s.landed) return s
    }
    return s
}
