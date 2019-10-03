/*
 * StreamTeam
 * Copyright (C) 2019  University of Basel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.unibas.dmi.dbis.streamTeam.sensorSimulator.helper;

/**
 * Helper class for calculating the match time.
 */
public class MatchTimeHelper {

    /**
     * Calculates the current soccer match timestamp (in ms) from the timestamp (in ms) the time provider returns.
     *
     * @param currentMachineTimestamp              Current machine timestamp (in ms) returned by the time provider
     * @param machineTimestampWhenStartingTheMatch Machine timestamp (in ms) returned by the time provider when simulation starts
     * @param matchStartTimestampInMs              Match timestamp (in ms) when the match started
     * @param simulationSpeedup                    Speedup multiplier for the simulation
     * @return Current soccer match timestamp (in ms)
     */
    public static long generateMatchTimestamp(long currentMachineTimestamp, long machineTimestampWhenStartingTheMatch, long matchStartTimestampInMs, double simulationSpeedup) {
        long machineDiff = currentMachineTimestamp - machineTimestampWhenStartingTheMatch;

        return (long) (matchStartTimestampInMs + machineDiff * simulationSpeedup);
    }


}
