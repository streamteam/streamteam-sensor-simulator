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

package ch.unibas.dmi.dbis.streamTeam.sensorSimulator.main;

import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.helper.ErrorCode;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.helper.ShutdownHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Starter for the SensorSimulator.
 */
public class SensorSimulatorStarter {

    /**
     * Slf4j logger
     */
    private static final Logger logger = LoggerFactory.getLogger(SensorSimulatorStarter.class);

    /**
     * Creates and starts the SensorSimulator.
     *
     * @param args Parameters
     */
    public static void main(String[] args) {
        ShutdownHelper.initialize();

        if (args.length < 5) {
            logger.error("Required parameters: <matchConfigFile> <matchId> <sensorDataFile> <startingTimestampInMs> <isMatchAnnouncer>\n");
            ShutdownHelper.shutdown(ErrorCode.WrongParameters);
        }

        Properties properties = new Properties();

        // Add properties from SensorSimulator config file
        String propertiesFilePath = "/sensorSimulator.properties";
        try {
            //http://stackoverflow.com/questions/29070109/how-to-read-properties-file-inside-jar
            InputStream in = SensorSimulatorStarter.class.getResourceAsStream(propertiesFilePath);
            properties.load(in);
        } catch (IOException e) {
            logger.error("Unable to load {}", propertiesFilePath, e);
            ShutdownHelper.shutdown(ErrorCode.PropertyException);
        }

        // Add properties from match config file
        String matchConfigFile = args[0];
        try {
            FileInputStream in = new FileInputStream(matchConfigFile);
            properties.load(in);
        } catch (IOException e) {
            logger.error("Unable to load {}", matchConfigFile, e);
            ShutdownHelper.shutdown(ErrorCode.PropertyException);
        }

        // Add properties from parameters
        properties.setProperty("match.id", args[1]);
        properties.setProperty("fileReader.sensorDataFile", args[2]);
        properties.setProperty("simulation.desiredMatchStartingMachineTimestampInMs", args[3]);
        properties.setProperty("streamWriter.kafka.isMatchAnnouncer", args[4]);

        SensorSimulator sensorSimulator = new SensorSimulator(properties);
        sensorSimulator.start();
    }
}
