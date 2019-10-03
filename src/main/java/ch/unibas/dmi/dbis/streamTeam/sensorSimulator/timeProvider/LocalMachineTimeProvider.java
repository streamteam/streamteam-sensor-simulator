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

package ch.unibas.dmi.dbis.streamTeam.sensorSimulator.timeProvider;

/**
 * Time provider implementation that simply returns the current local machine timestamp using System.currentTimeMillis().
 */
public class LocalMachineTimeProvider implements TimeProviderInterface {

    /**
     * LocalMachineTimeProvider constructor (does nothing).
     */
    public LocalMachineTimeProvider() {
        // NOTHING TO DO
    }

    /**
     * Starts the local machine time provider (does nothing).
     */
    @Override
    public void start() {
        // NOTHING TO DO
    }

    /**
     * Returns the current local machine timestamp (in ms) using System.currentTimeMillis().
     *
     * @return Current local machine timestamp (in ms)
     */
    @Override
    public long getTimeInMs() {
        return System.currentTimeMillis();
    }

    /**
     * Stops the local machine time provider (does nothing).
     */
    @Override
    public void close() {
        // NOTHING TO DO
    }

}
