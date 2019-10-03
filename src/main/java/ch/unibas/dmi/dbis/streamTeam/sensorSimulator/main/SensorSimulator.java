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

import ch.unibas.dmi.dbis.streamTeam.dataStreamElements.football.RawPositionSensorDataStreamElement;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.fileReader.TimedRawPositionSensorDataStreamElementReader;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.helper.MatchTimeHelper;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.helper.ShutdownHelper;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.helper.properties.PropertyReadHelper;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.streamWriter.StreamWriterInitializationException;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.streamWriter.StreamWriterInterface;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.streamWriter.StreamWriterType;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.timeProvider.TimeProviderInterface;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.timeProvider.TimeProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Main simulation class which reads data stream elements from the sensor data file and generates the sensor data stream w.r.t. the current match time.
 */
public class SensorSimulator {

    /**
     * Slf4j logger
     */
    private static final Logger logger = LoggerFactory.getLogger(SensorSimulator.class);

    /**
     * Properties
     */
    private final Properties properties;

    /**
     * The time provider instance
     */
    private TimeProviderInterface timeProvider;

    /**
     * The TimedRawPositionSensorDataStreamElementReader instance
     */
    private TimedRawPositionSensorDataStreamElementReader timedRawPositionSensorDataStreamElementReader;

    /**
     * SensorSimulator constructor.
     *
     * @param properties Properties
     */
    public SensorSimulator(Properties properties) {
        this.properties = properties;
    }

    /**
     * Start the sensor simulation.
     */
    public void start() {
        logger.info("Starting Sensor Simulator...");

        try {
            String timeProviderTypeString = PropertyReadHelper.readStringOrDie(this.properties, "timeProvider.type");
            TimeProviderType timeProviderType = TimeProviderType.valueOf(timeProviderTypeString.toUpperCase().trim());
            this.timeProvider = timeProviderType.getTimeProvider(this.properties);

            initializeTimeProvider();
            initializeTimedDataStreamElementReader();

            String streamWriterTypeString = PropertyReadHelper.readStringOrDie(this.properties, "streamWriter.type");
            StreamWriterType streamWriterType = StreamWriterType.valueOf(streamWriterTypeString.toUpperCase().trim());
            StreamWriterInterface streamWriter = streamWriterType.getStreamWriter(this.properties);

            streamWriter.initialize();
            ShutdownHelper.addCloseable(streamWriter);

            waitForTimeProvider();
            waitForDesiredMachineTimestamp();

            logger.info("Starting Simulation...");
            long actualMatchStartingMachineTimestampInMs = this.timeProvider.getTimeInMs();
            logger.debug("Starting machine time = {}", actualMatchStartingMachineTimestampInMs);

            long currentMachineTimestampInMs;
            long currentMatchTimestampInMs = 0;

            long matchStartTimestampInMs = PropertyReadHelper.readLongOrDie(this.properties, "match.time.startTs");
            long matchEndTimestampInMs = PropertyReadHelper.readLongOrDie(this.properties, "match.time.endTs");
            long dataStreamElementSendIntervalInMs = PropertyReadHelper.readLongOrDie(this.properties, "simulation.dataStreamElementSendIntervalInMs");
            double simulationSpeedup = PropertyReadHelper.readDoubleOrDie(this.properties, "simulation.speedup");

            // BEGIN MAIN SIMULATION LOOP
            while (currentMatchTimestampInMs <= matchEndTimestampInMs) {
                try {
                    Thread.sleep(dataStreamElementSendIntervalInMs);
                } catch (InterruptedException e) {
                    logger.error("Caught exception.", e);
                }

                currentMachineTimestampInMs = this.timeProvider.getTimeInMs();
                currentMatchTimestampInMs = MatchTimeHelper.generateMatchTimestamp(currentMachineTimestampInMs, actualMatchStartingMachineTimestampInMs, matchStartTimestampInMs, simulationSpeedup);
                try {
                    List<RawPositionSensorDataStreamElement> newDataStreamElements = this.timedRawPositionSensorDataStreamElementReader.readDataStreamElementsProducedBeforeOrAt(currentMatchTimestampInMs);
                    logger.debug("Number of read data stream elements at timestamp {}: {}", currentMatchTimestampInMs, newDataStreamElements.size());
                    streamWriter.sendDataStreamElements(newDataStreamElements);
                } catch (IOException e) {
                    logger.error("Caught exception.", e);
                }
            }
            // END MAIN SIMULATION LOOP

            streamWriter.close();
            ShutdownHelper.removeClosable(streamWriter);

        } catch (StreamWriterInitializationException | IllegalArgumentException e) {
            logger.error("Caught exception.", e);
        }

        closeTimedDataStreamElementReader();
        closeTimeProvider();
    }

    /**
     * Initializes the TimedRawPositionSensorDataStreamElementReader.
     */
    private void initializeTimedDataStreamElementReader() {
        logger.info("Initializing TimedRawPositionSensorDataStreamElementReader...");
        File sensorDataFile = new File(PropertyReadHelper.readStringOrDie(this.properties, "fileReader.sensorDataFile"));
        this.timedRawPositionSensorDataStreamElementReader = new TimedRawPositionSensorDataStreamElementReader(this.properties, sensorDataFile);
        try {
            this.timedRawPositionSensorDataStreamElementReader.initialize();
            ShutdownHelper.addCloseable(this.timedRawPositionSensorDataStreamElementReader);
        } catch (IOException e) {
            logger.error("Caught exception.", e);
        }
    }

    /**
     * Closes the TimedRawPositionSensorDataStreamElementReader.
     */
    private void closeTimedDataStreamElementReader() {
        logger.info("Closing TimedRawPositionSensorDataStreamElementReader...");
        try {
            this.timedRawPositionSensorDataStreamElementReader.close();
            ShutdownHelper.removeClosable(this.timedRawPositionSensorDataStreamElementReader);
        } catch (IOException e) {
            logger.error("Caught exception.", e);
        }
    }

    /**
     * Initializes the TimeProvider.
     */
    private void initializeTimeProvider() {
        try {
            this.timeProvider.start();
            ShutdownHelper.addCloseable(this.timeProvider);
        } catch (Exception e) {
            logger.error("Caught exception.", e);
        }
    }

    /**
     * Waits until the TimeProvider has initialized.
     */
    private void waitForTimeProvider() {
        logger.info("Waiting for the Time Provider to initialize...");
        try {
            long timeProviderInitializationTimeInMs = PropertyReadHelper.readLongOrDie(this.properties, "timeProvider.initializationTimeInMs");
            Thread.sleep(timeProviderInitializationTimeInMs);
        } catch (InterruptedException e) {
            logger.error("Caught exception.", e);
        }
    }

    /**
     * Closes the Timeprovider.
     */
    private void closeTimeProvider() {
        logger.info("Closing TimeProvider");
        this.timeProvider.close();
        ShutdownHelper.removeClosable(this.timeProvider);
    }

    /**
     * Waits until the machine has the desired starting timestamp.
     */
    private void waitForDesiredMachineTimestamp() {
        logger.info("Waiting for desired machine timestamp to pass before starting the simulation...");
        long checkBeforeStartIntervalInMs = PropertyReadHelper.readLongOrDie(this.properties, "simulation.checkBeforeStartIntervalInMs");
        long desiredMachineTimestampForStartingTheMatch = PropertyReadHelper.readLongOrDie(this.properties, "simulation.desiredMatchStartingMachineTimestampInMs");
        long curTs = this.timeProvider.getTimeInMs();
        logger.info("Desired: {}, Current: {}, Difference: {}", desiredMachineTimestampForStartingTheMatch, curTs, desiredMachineTimestampForStartingTheMatch - curTs);
        while (this.timeProvider.getTimeInMs() < desiredMachineTimestampForStartingTheMatch) {
            try {
                Thread.sleep(checkBeforeStartIntervalInMs);
            } catch (InterruptedException e) {
                logger.error("Caught exception.", e);
            }
        }
    }

}
