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

package ch.unibas.dmi.dbis.streamTeam.sensorSimulator.streamWriter;

import ch.unibas.dmi.dbis.streamTeam.dataStreamElements.football.RawPositionSensorDataStreamElement;

import java.io.Closeable;
import java.util.List;

/**
 * Stream writer Interface.
 */
public interface StreamWriterInterface extends Closeable {

    /**
     * Initializes the StreamWriter.
     *
     * @throws StreamWriterInitializationException Thrown if the StreamWriterInterface implementation could not have been initialized.
     */
    void initialize() throws StreamWriterInitializationException;

    /**
     * Sends a list of rawPositionSensorData stream elements.
     *
     * @param dataStreamElements List of rawPositionSensorData stream elements
     */
    void sendDataStreamElements(List<RawPositionSensorDataStreamElement> dataStreamElements);

    /**
     * Closes the StreamWriter.
     */
    void close();
}
