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


import ch.unibas.dmi.dbis.streamTeam.dataStreamElements.AbstractImmutableDataStreamElement;
import ch.unibas.dmi.dbis.streamTeam.dataStreamElements.football.MatchMetadataStreamElement;
import ch.unibas.dmi.dbis.streamTeam.dataStreamElements.football.RawPositionSensorDataStreamElement;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.helper.properties.PropertyReadHelper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

/**
 * A StreamWriterInterface implementation for sending new data stream elements to a Kafka topic.
 */
public class KafkaStreamWriter implements StreamWriterInterface {

    /**
     * Slf4j logger
     */
    private static final Logger logger = LoggerFactory.getLogger(KafkaStreamWriter.class);

    /**
     * Properties
     */
    private final Properties properties;

    /**
     * KafkaProducer
     */
    private Producer<String, String> producer;

    /**
     * Identifier of the match
     */
    private String matchId;

    /**
     * KafkaStreamWriter constructor.
     *
     * @param properties Properties
     */
    public KafkaStreamWriter(Properties properties) {
        this.properties = properties;
    }

    /**
     * Initializes the KafkaStreamWriter.
     */
    @Override
    public void initialize() {
        // https://kafka.apache.org/090/javadoc/index.html?org/apache/kafka/clients/producer/KafkaProducer.html
        // https://kafka.apache.org/082/documentation.html#newproducerconfigs

        String brokerList = PropertyReadHelper.readStringOrDie(this.properties, "streamWriter.kafka.brokerList");
        this.matchId = PropertyReadHelper.readStringOrDie(this.properties, "match.id");

        Properties props = new Properties();
        props.put("bootstrap.servers", brokerList);
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 0); // wait 0ms for batching
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        this.producer = new KafkaProducer<>(props);

        boolean isMatchAnnouncer = PropertyReadHelper.readBooleanOrDie(this.properties, "streamWriter.kafka.isMatchAnnouncer");

        String dummyTopic = PropertyReadHelper.readStringOrDie(this.properties, "streamWriter.kafka.initializeTopic");

        // Write a dummy record to a dummy topic in order to prevent that the first data stream elements are sent in batches
        String dummyString = "dummy";
        this.producer.send(new ProducerRecord(dummyTopic, this.matchId, dummyString.getBytes()));
        logger.info("Sent dummy record to dummy topic.");

        if (isMatchAnnouncer) {
            long matchStartTimestampInMs = PropertyReadHelper.readLongOrDie(this.properties, "match.time.startTs");
            String sport = PropertyReadHelper.readStringOrDie(this.properties, "match.sport");
            double fieldLength = PropertyReadHelper.readDoubleOrDie(this.properties, "match.fieldLength");
            double fieldWidth = PropertyReadHelper.readDoubleOrDie(this.properties, "match.fieldWidth");
            boolean mirroredX = PropertyReadHelper.readBooleanOrDie(this.properties, "match.mirroredX");
            boolean mirroredY = PropertyReadHelper.readBooleanOrDie(this.properties, "match.mirroredY");
            String areaInfos = PropertyReadHelper.readStringOrDie(this.properties, "match.areaInfos");
            String competition = PropertyReadHelper.readStringOrDie(this.properties, "match.competition");
            String venue = PropertyReadHelper.readStringOrDie(this.properties, "match.venue");
            String objectRenameMap = PropertyReadHelper.readStringOrDie(this.properties, "match.objectRenameMap");
            String teamRenameMap = PropertyReadHelper.readStringOrDie(this.properties, "match.teamRenameMap");
            String videoPath = PropertyReadHelper.readStringOrDie(this.properties, "match.video.path");
            int videoOffset = PropertyReadHelper.readIntOrDie(this.properties, "match.video.offset");
            String teamColors = PropertyReadHelper.readStringOrDie(this.properties, "match.teamColors");

            long matchStartUnixTs = System.currentTimeMillis();

            try {
                MatchMetadataStreamElement matchMetadataStreamElement = MatchMetadataStreamElement.generateMatchMetadataStreamElement(this.matchId, matchStartTimestampInMs, matchStartTimestampInMs, sport, fieldLength, fieldWidth, mirroredX, mirroredY, areaInfos, matchStartUnixTs, competition, venue, objectRenameMap, teamRenameMap, videoPath, videoOffset, teamColors);
                this.producer.send(new ProducerRecord(matchMetadataStreamElement.getStreamName(), matchMetadataStreamElement.getKey(), matchMetadataStreamElement.getContentAsByteArray()));
                logger.info("Sent matchMetadata stream element.");
            } catch (AbstractImmutableDataStreamElement.CannotGenerateDataStreamElement e) {
                logger.error("Error during generating matchMetadata stream element: ", e);
            }
        }

        this.producer.flush();
        logger.info("Flushed producer.");
    }

    /**
     * Sends a list of rawPositionSensorData stream elements to Kafka.
     *
     * @param dataStreamElements List of rawPositionSensorData stream elements
     */
    @Override
    public void sendDataStreamElements(List<RawPositionSensorDataStreamElement> dataStreamElements) {
        for (RawPositionSensorDataStreamElement dataStreamElement : dataStreamElements) {
            this.producer.send(new ProducerRecord(dataStreamElement.getStreamName(), dataStreamElement.getKey(), dataStreamElement.getContentAsByteArray()));
        }
        this.producer.flush();
    }

    /**
     * Closes the KafkaStreamWriter.
     */
    @Override
    public void close() {
        this.producer.close();
    }
}
