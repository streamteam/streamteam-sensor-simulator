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

package ch.unibas.dmi.dbis.streamTeam.sensorSimulator.fileReader;

import ch.unibas.dmi.dbis.streamTeam.dataStreamElements.football.RawPositionSensorDataStreamElement;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.fileReader.helper.DataStreamElementFromLineFactoryInterface;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.fileReader.helper.EmptyBufferException;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.fileReader.helper.PreBufferedDataStreamElementFileReader;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * A PreBufferedDataStreamElementFileReader implementation which supports retreiving all rawPositionSensorData stream elements from the buffer which are measured before a given match timestamp (in ms).
 */
public class TimedRawPositionSensorDataStreamElementReader extends PreBufferedDataStreamElementFileReader<RawPositionSensorDataStreamElement> {

    /**
     * TimedRawPositionSensorDataStreamElementReader constructor.
     *
     * @param properties Properties
     * @param file       File that have to be read
     */
    public TimedRawPositionSensorDataStreamElementReader(Properties properties, File file) {
        super(properties, file);
    }

    /**
     * Generates the factory for generating a rawPositionSensorData stream element from a single line.
     *
     * @return Factory
     */
    @Override
    protected DataStreamElementFromLineFactoryInterface<RawPositionSensorDataStreamElement> generateFactory() {
        return new RawPositionSensorDataStreamElementFromLineFactory(this.properties);
    }

    /**
     * Retrieves a list containing all rawPositionSensorData stream elements from the buffer which are measured before a given match timestamp (in ms).
     *
     * @param timestamp Match timestamp (in ms)
     * @return rawPositionSensorData stream elements
     * @throws IOException Thrown in case of an IOException while filling the buffer.
     */
    public List<RawPositionSensorDataStreamElement> readDataStreamElementsProducedBeforeOrAt(long timestamp) throws IOException {
        List<RawPositionSensorDataStreamElement> res = new LinkedList<>();

        boolean done = false;

        while (!done) {
            try {
                RawPositionSensorDataStreamElement newRawPositionSensorDataStreamElement = this.pollElementFromBuffer();
                if (newRawPositionSensorDataStreamElement.getGenerationTimestamp() <= timestamp) {
                    res.add(newRawPositionSensorDataStreamElement);
                } else {
                    this.addToFirstPositionAtTheBuffer(newRawPositionSensorDataStreamElement); // put it back!!! otherwise we lose the data stream element!
                    done = true;
                }
            } catch (EmptyBufferException e) {
                done = true;
            }
        }

        return res;
    }

}
