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

package ch.unibas.dmi.dbis.streamTeam.tracabFileTransformator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

/**
 * Transformator for transforming TRACAB files to our sensor simulator files.
 */
public class TracabFileTransformator {

    /**
     * Slf4j logger
     */
    private static final Logger logger = LoggerFactory.getLogger(TracabFileTransformator.class);


    /**
     * Main method for performing the parameters.
     *
     * @param args trackingDataFilePath and metaDataFilePath
     */
    public static void main(String[] args) {
        String trackingDataFilePath = args[0];
        logger.info("Tracking data file: {}", trackingDataFilePath);
        String metaDataFilePath = args[1];
        logger.info("Meta data file: {}", trackingDataFilePath);

        try {
            //===========================
            //=== READ META DATA FILE ===
            //===========================
            logger.info("Starts reading meta data file.");
            // https://www.tutorialspoint.com/java_xml/java_dom_parse_document.htm
            File metaDataFile = new File(metaDataFilePath);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(metaDataFile);
            NodeList matchNodes = document.getElementsByTagName("match");
            Element matchElement = (Element) matchNodes.item(0);
            double xSize = Double.parseDouble(matchElement.getAttribute("fPitchXSizeMeters"));
            double ySize = Double.parseDouble(matchElement.getAttribute("fPitchYSizeMeters"));
            int fps = Integer.parseInt(matchElement.getAttribute("iFrameRateFps"));
            NodeList periodNodes = matchElement.getElementsByTagName("period");
            Element firstPeriod = (Element) periodNodes.item(0);
            int startFrameNumber = Integer.parseInt(firstPeriod.getAttribute("iStartFrame"));
            int endFrameNumber = Integer.parseInt(firstPeriod.getAttribute("iEndFrame"));

            //===============================
            //=== READ TRACKING DATA FILE ===
            //===============================
            logger.info("Starts reading tracking data file.");
            List<Integer> homePlayerIds = new ArrayList<>();
            List<Integer> awayPlayerIds = new ArrayList<>();

            Map<Integer, List<String>> playerLines = new HashMap<>();
            List<String> ballLines = new ArrayList<>();

            File trackingDataFile = new File(trackingDataFilePath);
            FileReader fileReader = new FileReader(trackingDataFile);
            BufferedReader bufferedFileReader = new BufferedReader(fileReader);

            String line = bufferedFileReader.readLine();
            while (line != null) {
                String[] chunks = line.split(":");

                int frameNumber = Integer.parseInt(chunks[0]);

                if (frameNumber > startFrameNumber) { // only from start of first halftime...
                    if (frameNumber > endFrameNumber) { // ...until end of first halftime
                        break;
                    }

                    // Calculate timestamp
                    int ts = (int) ((frameNumber - startFrameNumber) * (((double) 1000) / fps));
                    String tsString = Integer.toString(ts);

                    // Extract player data and generate line for player sensor files
                    String[] players = chunks[1].split(";");
                    for (String player : players) {
                        if (player.length() > 2) {
                            String[] playerParts = player.split(",");

                            Integer playerId = Integer.parseInt(playerParts[2]); // player id = jersey number (+100 if away team) since the tracking id seems to change during the game

                            if (playerParts[0].equals("1")) { // HOME
                                if (!homePlayerIds.contains(playerId)) {
                                    homePlayerIds.add(playerId);
                                    playerLines.put(playerId, new ArrayList<>());
                                }
                            } else if (playerParts[0].equals("0")) { // AWAY
                                playerId += 100;
                                if (!awayPlayerIds.contains(playerId)) {
                                    awayPlayerIds.add(playerId);
                                    playerLines.put(playerId, new ArrayList<>());
                                }
                            } else {
                                continue; // skip player
                            }

                            int playerX = Integer.parseInt(playerParts[3]);
                            int playerY = Integer.parseInt(playerParts[4]);
                            int playerZ = 0; // no z data for player

                            playerLines.get(playerId).add(generateLine(tsString, playerId, playerX, playerY, playerZ));
                        }
                    }

                    // Extract ball data and generate line for ball sensor file
                    String[] ballParts = chunks[2].split(";")[0].split(",");
                    int ballX = Integer.parseInt(ballParts[0]);
                    int ballY = Integer.parseInt(ballParts[1]);
                    int ballZ = Integer.parseInt(ballParts[2]);
                    ballLines.add(generateLine(tsString, 200, ballX, ballY, ballZ));
                }
                line = bufferedFileReader.readLine();
            }
            bufferedFileReader.close();
            fileReader.close();

            //===================
            //=== WRITE FILES ===
            //===================
            logger.info("Starts writing files.");
            Collections.sort(homePlayerIds);
            Collections.sort(awayPlayerIds);
            writeSensorFile("ball", 200, ballLines);
            for (Integer homePlayerId : homePlayerIds) {
                writeSensorFile("home", homePlayerId, playerLines.get(homePlayerId));
            }
            for (Integer awayPlayerId : awayPlayerIds) {
                writeSensorFile("away", awayPlayerId, playerLines.get(awayPlayerId));
            }
            writeConfig(startFrameNumber, endFrameNumber, fps, xSize, ySize, homePlayerIds, awayPlayerIds);
            writeSids(homePlayerIds, awayPlayerIds);
            logger.info("Finished.");
        } catch (SAXException | ParserConfigurationException | IOException e) {
            logger.error("Caught exception.", e);
        }
    }

    /**
     * Generates a line for a sensor file.
     *
     * @param ts Timestamp
     * @param id Player or ball identifier
     * @param x  X position in cm
     * @param y  Y position in cm
     * @param z  Z position in cm
     * @return Line for sensor file
     */
    public static String generateLine(String ts, int id, int x, int y, int z) {
        // cm -> m
        Double xInM = ((double) x) / 100;
        Double yInM = ((double) y) / 100;
        Double zInM = ((double) z) / 100;

        StringBuilder playerLineBuilder = new StringBuilder(ts);
        playerLineBuilder.append(",");
        playerLineBuilder.append(xInM);
        playerLineBuilder.append(",");
        playerLineBuilder.append(yInM);
        playerLineBuilder.append(",");
        playerLineBuilder.append(zInM);
        playerLineBuilder.append(",");
        playerLineBuilder.append(id);
        return playerLineBuilder.toString();
    }

    /**
     * Writes a sensor file.
     *
     * @param team     Team (ball/home/away)
     * @param id       Player or ball identifier
     * @param lineList List of lines for the sensor file
     * @throws IOException All potential IOExceptions
     */
    public static void writeSensorFile(String team, int id, List<String> lineList) throws IOException {
        File outputDir = new File("output/" + team);
        outputDir.mkdirs();
        File outputFile = new File("output/" + team + "/" + id + ".csv");
        if (outputFile.exists()) {
            outputFile.delete();
        }
        outputFile.createNewFile();
        FileWriter fileWriter = new FileWriter(outputFile);
        fileWriter.write("\"Timestamp\",\"X\",\"Y\",\"Z\",\"ID\"\n");
        for (String line : lineList) {
            fileWriter.write(line + "\n");
        }
        fileWriter.close();
    }

    /**
     * Writes a config.properties file.
     *
     * @param startFrameNumber Number of the first frame for the first halftime
     * @param endFrameNumber   Number of the last frame for the first halftime
     * @param fps              Frames per second
     * @param xSize            Field x size in m
     * @param ySize            Field y size in m
     * @param homePlayerIds    List containing the identifiers of all home players
     * @param awayPlayerIds    List containing the identifiers of all away players
     * @throws IOException All potential IOExceptions
     */
    public static void writeConfig(int startFrameNumber, int endFrameNumber, int fps, double xSize, double ySize, List<Integer> homePlayerIds, List<Integer> awayPlayerIds) throws IOException {
        File configFile = new File("output/config.properties");
        if (configFile.exists()) {
            configFile.delete();
        }
        configFile.createNewFile();
        FileWriter fileWriter = new FileWriter(configFile);
        fileWriter.write("# Timestamp of the match start (in ms) (a little earlier than the first data stream elements)\n");
        fileWriter.write("match.time.startTs = 0\n");
        fileWriter.write("\n");
        fileWriter.write("# Timestamp of the match end (in ms)\n");
        int endTs = (int) ((endFrameNumber - startFrameNumber) * (((double) 1000) / fps));
        fileWriter.write("match.time.endTs = " + endTs + "\n");
        fileWriter.write("\n");
        fileWriter.write("# Path to the video file of the match\n");
        fileWriter.write("match.video.path = TODO\n");
        fileWriter.write("\n");
        fileWriter.write("# Offset of the video of the match (in s)\n");
        fileWriter.write("match.video.offset = 1337\n");
        fileWriter.write("\n");
        fileWriter.write("# Map of sids to object identifiers\n");
        fileWriter.write("match.objectRenameMap={200:BALL:Ball}");
        int i = 1;
        for (Integer homePlayerId : homePlayerIds) {
            fileWriter.write("%{" + homePlayerId + ":A" + i + ":TODONAMEPLAYERA" + i + "}");
            i++;
        }
        i = 1;
        for (Integer awayPlayerId : awayPlayerIds) {
            fileWriter.write("%{" + awayPlayerId + ":B" + i + ":TODONAMEPLAYERB" + i + "}");
            i++;
        }
        fileWriter.write("\n");
        fileWriter.write("\n");
        fileWriter.write("# Map of team folders to team identifiers\n");
        fileWriter.write("match.teamRenameMap={ball:BALL:Ball}%{home:A:TODONAMETEAMA}%{away:B:TODONAMETEAMB}\n");
        fileWriter.write("\n");
        fileWriter.write("# Map of team identifiers to colors\n");
        fileWriter.write("match.teamColors={A:blue}%{B:red}\n");
        fileWriter.write("\n");
        fileWriter.write("# Discipline of the match\n");
        fileWriter.write("match.sport = football\n");
        fileWriter.write("\n");
        fileWriter.write("# Length of the field in m\n");
        fileWriter.write("match.fieldLength = " + xSize + "\n");
        fileWriter.write("\n");
        fileWriter.write("# Width of the field in m\n");
        fileWriter.write("match.fieldWidth = " + ySize + "\n");
        fileWriter.write("\n");
        fileWriter.write("# Boolean which specifies if the x coordinate of the tracked positions is mirrored\n");
        fileWriter.write("match.mirroredX = TODO\n");
        fileWriter.write("\n");
        fileWriter.write("# Boolean which specifies if the y coordinate of the tracked positions is mirrored\n");
        fileWriter.write("match.mirroredY = TODO\n");
        fileWriter.write("\n");
        fileWriter.write("# List of area infos, each of the form areaId:minX@maxX@minY@maxY\n");
        fileWriter.write("match.areaInfos = " + createAreaInfosString(xSize, ySize) + "\n");
        fileWriter.write("\n");
        fileWriter.write("# Competition\n");
        fileWriter.write("match.competition = TODO\n");
        fileWriter.write("\n");
        fileWriter.write("# Venue\n");
        fileWriter.write("match.venue = TODO\n");
        fileWriter.close();
    }

    /**
     * Creates area infos string definition.
     *
     * @param fieldLength Field length
     * @param fieldWidth  Field width
     * @return Area infos string definition
     */
    public static String createAreaInfosString(double fieldLength, double fieldWidth) {
        double minX = -fieldLength / 2;
        double maxX = fieldLength / 2;
        double minY = -fieldWidth / 2;
        double maxY = fieldWidth / 2;

        StringBuilder sb = new StringBuilder();
        sb.append(createAreaInfoString("field", minX, maxX, minY, maxY));
        sb.append(",");
        sb.append(createAreaInfoString("leftThird", minX, minX / 3, minY, maxY));
        sb.append(",");
        sb.append(createAreaInfoString("centerThird", minX / 3, maxX / 3, minY, maxY));
        sb.append(",");
        sb.append(createAreaInfoString("rightThird", maxX / 3, maxX, minY, maxY));
        sb.append(",");
        sb.append(createAreaInfoString("aboveLeftThird", minX, minX / 3, minY - 5, minY));
        sb.append(",");
        sb.append(createAreaInfoString("aboveCenterThird", minX / 3, maxX / 3, minY - 5, minY));
        sb.append(",");
        sb.append(createAreaInfoString("aboveRightThird", maxX / 3, maxX, minY - 5, minY));
        sb.append(",");
        sb.append(createAreaInfoString("belowLeftThird", minX, minX / 3, maxY, maxY + 5));
        sb.append(",");
        sb.append(createAreaInfoString("belowCenterThird", minX / 3, maxX / 3, maxY, maxY + 5));
        sb.append(",");
        sb.append(createAreaInfoString("belowRightThird", maxX / 3, maxX, maxY, maxY + 5));
        sb.append(",");
        sb.append(createAreaInfoString("leftPenaltyBox", minX, minX + 16.5, -20.16, 20.16));
        sb.append(",");
        sb.append(createAreaInfoString("rightPenaltyBox", maxX - 16.5, maxX, -20.16, 20.16));
        sb.append(",");
        sb.append(createAreaInfoString("belowLeftGoal", minX - 5, minX, 9.16, maxY + 5));
        sb.append(",");
        sb.append(createAreaInfoString("slightlyBelowLeftGoal", minX - 5, minX, 3.66, 9.16));
        sb.append(",");
        sb.append(createAreaInfoString("leftGoal", minX - 5, minX, -3.66, 3.66));
        sb.append(",");
        sb.append(createAreaInfoString("slightlyAboveLeftGoal", minX - 5, minX, -9.16, -3.66));
        sb.append(",");
        sb.append(createAreaInfoString("aboveLeftGoal", minX - 5, minX, minY - 5, -9.16));
        sb.append(",");
        sb.append(createAreaInfoString("belowRightGoal", maxX, maxX + 5, 9.16, maxY + 5));
        sb.append(",");
        sb.append(createAreaInfoString("slightlyBelowRightGoal", maxX, maxX + 5, 3.66, 9.16));
        sb.append(",");
        sb.append(createAreaInfoString("rightGoal", maxX, maxX + 5, -3.66, 3.66));
        sb.append(",");
        sb.append(createAreaInfoString("slightlyAboveRightGoal", maxX, maxX + 5, -9.16, -3.66));
        sb.append(",");
        sb.append(createAreaInfoString("aboveRightGoal", maxX, maxX + 5, minY - 5, -9.16));
        sb.append(",");
        sb.append(createAreaInfoString("leftBottomCorner", minX - 3, minX + 3, maxY - 3, maxY + 3));
        sb.append(",");
        sb.append(createAreaInfoString("rightBottomCorner", maxX - 3, maxX + 3, maxY - 3, maxY + 3));
        sb.append(",");
        sb.append(createAreaInfoString("leftTopCorner", minX - 3, minX + 3, minY - 3, minY + 3));
        sb.append(",");
        sb.append(createAreaInfoString("rightTopCorner", maxX - 3, maxX + 3, minY - 3, minY + 3));
        return sb.toString();
    }

    /**
     * Creates the string definition of a single area info.
     *
     * @param areaId Identifier of the area
     * @param minX   Minimal x position of the rectangular area
     * @param maxX   Maximal x position of the rectangular area
     * @param minY   Minimal y position of the rectangular area
     * @param maxY   Maximal y position of the rectangular area
     * @return String definition of a single area info
     */
    public static String createAreaInfoString(String areaId, double minX, double maxX, double minY, double maxY) {
        StringBuilder sb = new StringBuilder("{");
        sb.append(areaId);
        sb.append(":");
        sb.append(minX);
        sb.append("@");
        sb.append(maxX);
        sb.append("@");
        sb.append(minY);
        sb.append("@");
        sb.append(maxY);
        sb.append("}");
        return sb.toString();
    }

    /**
     * Writes sids.py file.
     *
     * @param homePlayerIds List containing the identifiers of all home players
     * @param awayPlayerIds List containing the identifiers of all away players
     * @throws IOException All potential IOExceptions
     */
    public static void writeSids(List<Integer> homePlayerIds, List<Integer> awayPlayerIds) throws IOException {
        File sidsFile = new File("output/sids.py");
        if (sidsFile.exists()) {
            sidsFile.delete();
        }
        sidsFile.createNewFile();
        FileWriter fileWriter = new FileWriter(sidsFile);

        fileWriter.write("sids = [");
        for (Integer homePlayerId : homePlayerIds) {
            fileWriter.write("\"home/" + homePlayerId + "\", ");
        }
        for (Integer awayPlayerId : awayPlayerIds) {
            fileWriter.write("\"away/" + awayPlayerId + "\", ");
        }
        fileWriter.write("\"ball/200\"]");
        fileWriter.close();
    }
}
