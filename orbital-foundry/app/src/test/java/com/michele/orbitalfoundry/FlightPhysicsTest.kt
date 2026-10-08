package com.michele.orbitalfoundry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class FlightPhysicsTest {
    @Test
    fun circularOrbitAtTwoHundredKmHasExpectedApsides() {
        val radius = EARTH_RADIUS_KM + 200.0
        val circularSpeed = sqrt(EARTH_MU / radius)
        val orbit = orbitalMetrics(V2(radius, 0.0), V2(0.0, circularSpeed))

        assertFalse(orbit.escape)
        assertEquals(200.0, orbit.apoapsisAltitudeKm, 0.01)
        assertEquals(200.0, orbit.periapsisAltitudeKm, 0.01)
    }

    @Test
    fun stagingJettisonsLowerStageAndActivatesUpperStage() {
        val parts = listOf(
            PartType.CAPSULE, PartType.TANK, PartType.ENGINE,
            PartType.DECOUPLER, PartType.TANK, PartType.ENGINE, PartType.FIN
        )
        val state = SimState(
            pos = V2(EARTH_RADIUS_KM + 100.0, 0.0),
            vel = V2(0.0, 7.8),
            fuel = 24.0,
            mass = 40.0,
            thrust = 1250.0,
            parts = parts,
            throttle = 0.8
        )

        val next = separateStage(state)

        assertEquals(parts.take(3), next.parts)
        assertEquals(2, next.stage)
        assertEquals(24.0, next.fuel, 0.001)
        assertEquals(1250.0, next.thrust, 0.001)
        assertEquals(0.0, next.throttle, 0.001)
    }

    @Test
    fun dockingRequiresCloseRangeAndMatchedRelativeVelocity() {
        val rocket = Rocket(listOf(
            PartType.CAPSULE, PartType.DOCKING_PORT, PartType.RCS,
            PartType.TANK, PartType.ENGINE
        ))
        var state = beginDockingPractice(rocket)
        assertTrue(state.docking)
        assertFalse(state.docked)

        state = applyDockingApproach(state)
        var caught = false
        repeat(2000) {
            if (!caught) {
                state = stepPhysics(state, 0.15, 90.0)
                val target = state.targetPos ?: return@repeat
                if ((target - state.pos).mag() * 1000.0 <= 4.0) caught = true
            }
        }

        assertTrue("Approach should close the initial ~40 m gap", caught)
        state = matchDockingVelocity(state)
        state = attemptDock(state)
        assertTrue("Dock should succeed within 5 m with matched relative speed", state.docked)
    }

    @Test
    fun defaultVehicleHasPositiveDeltaVAndUsableThrust() {
        val starter = Rocket(listOf(
            PartType.NOSE, PartType.CAPSULE, PartType.HEATSHIELD, PartType.PARACHUTE,
            PartType.TANK, PartType.ENGINE, PartType.DECOUPLER, PartType.TANK, PartType.ENGINE, PartType.FIN
        ))

        assertTrue(starter.deltaV > 0.0)
        assertTrue(activeStageParts(starter.parts).sumOf { it.thrust } > 0.0)
        assertTrue(starter.fuel > 0.0)
    }
}
