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

import ch.unibas.dmi.dbis.streamTeam.dataStreamElements.AbstractImmutableDataStreamElement;
import ch.unibas.dmi.dbis.streamTeam.dataStreamElements.football.RawPositionSensorDataStreamElement;
import ch.unibas.dmi.dbis.streamTeam.dataStructures.Geometry;
import ch.unibas.dmi.dbis.streamTeam.dataStructures.ObjectInfo;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.fileReader.helper.DataStreamElementFromLineFactoryInterface;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.helper.properties.PropertyReadHelper;

import java.util.Properties;

/**
 * Factory to generate a rawPositionSensorData stream element from a single line (string).
 */
public class RawPositionSensorDataStreamElementFromLineFactory implements DataStreamElementFromLineFactoryInterface<RawPositionSensorDataStreamElement> {

    /**
     * Identifier of the match
     */
    private String matchId;

    /**
     * Identifier of the team
     */
    private String teamId;

    /**
     * RawPositionSensorDataStreamElementFromLineFactory constructor.
     *
     * @param properties Properties
     */
    public RawPositionSensorDataStreamElementFromLineFactory(Properties properties) {
        this.matchId = PropertyReadHelper.readStringOrDie(properties, "match.id");
        String sensorDataFile = PropertyReadHelper.readStringOrDie(properties, "fileReader.sensorDataFile");
        this.teamId = sensorDataFile.split("/")[3];
    }

    /**
     * Generates a rawPositionSensorData stream element given a single line (string).
     *
     * @param line Line (string) that specifies the rawPositionSensorData stream element
     * @return rawPositionSensorData stream element that is generated using the given line
     * @throws AbstractImmutableDataStreamElement.CannotGenerateDataStreamElement Thrown if the rawPositionSensorData stream element could not be generated
     */
    public RawPositionSensorDataStreamElement generateFromLine(String line) throws AbstractImmutableDataStreamElement.CannotGenerateDataStreamElement {
        // Replace missing x and y values with 0
        String correctedLine = line.replaceAll(",,", ",0,");

        // Remove " around playerId
        String correctedLine2 = correctedLine.replaceAll("\"", "");

        String[] splittedLine = correctedLine2.split(",");

        long generationTimestamp = Long.parseLong(splittedLine[0]);
        double x = Double.parseDouble(splittedLine[1]);
        double y = Double.parseDouble(splittedLine[2]);
        double z = Double.parseDouble(splittedLine[3]);
        String objectId = splittedLine[4];

        ObjectInfo objectInfo = new ObjectInfo(objectId, this.teamId, new Geometry.Vector(x, y, z));

        return RawPositionSensorDataStreamElement.generateRawPositionSensorDataStreamElement(this.matchId, generationTimestamp, objectInfo);
    }

}
