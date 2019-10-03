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

import java.io.Closeable;

/**
 * Time provider interface.
 */
public interface TimeProviderInterface extends Closeable {

    /**
     * Starts the time provider.
     *
     * @throws Exception Thrown if an Exception is thrown during startup.
     */
    void start() throws Exception;

    /**
     * Returns the current time (in ms).
     *
     * @return Current time (in ms)
     */
    long getTimeInMs();

    /**
     * Stops the time provider.
     */
    @Override
    void close();
}
