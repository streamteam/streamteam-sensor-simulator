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

package ch.unibas.dmi.dbis.streamTeam.sensorSimulator.fileReader.helper;

import ch.unibas.dmi.dbis.streamTeam.dataStreamElements.AbstractImmutableDataStreamElement;
import ch.unibas.dmi.dbis.streamTeam.sensorSimulator.helper.properties.PropertyReadHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.LinkedList;
import java.util.Properties;

/**
 * A reader which reads larger blocks of lines, converts them into an implementation of AbstractImmutableDataStreamElement using an implementation of the DataStreamElementFromLineFactoryInterface, and stores them in a buffer.
 * The size of the buffer is always between Constants.MAX_BUFFER_SIZE and Constants.MIN_BUFFER_SIZE.
 * After initializing the PreBufferedDataStreamElementFileReader the buffer is filled (size = MAX_BUFFER_SIZE).
 * When the buffer size falls below MIN_BUFFER_SIZE, it is refilled up to its maximum size.
 * Exception: The buffer size is allowed to fall below MIN_BUFFER_SIZE if the reader reaches the end of the file.
 * Always initialize() the PreBufferedDataStreamElementFileReader before call pollElementFromBuffer() and close() it after reading the last element.
 *
 * @param <T> Implementation of AbstractImmutableDataStreamElement
 */
public abstract class PreBufferedDataStreamElementFileReader<T extends AbstractImmutableDataStreamElement> implements Closeable {

    /**
     * Slf4j logger
     */
    private static final Logger logger = LoggerFactory.getLogger(PreBufferedDataStreamElementFileReader.class);

    /**
     * Properties
     */
    protected final Properties properties;

    /**
     * File that have to be read
     */
    private final File file;

    /**
     * FileReader for reading the file
     */
    private FileReader fileReader;

    /**
     * BufferedReader for reading the file
     */
    private BufferedReader bufferedReader;

    /**
     * Factory for generating data stream elements given a line of the file (string)
     */
    private DataStreamElementFromLineFactoryInterface<T> factory;

    /**
     * Minimal number of elements in the buffer (if the end of the file is not reached yet)
     */
    private int minBufferSize;

    /**
     * Maximal number of elements in the buffer
     */
    private int maxBufferSize;

    /**
     * Buffer storing prefetched objects
     */
    private LinkedList<T> buffer;

    /**
     * Reflects if the end of the file is already reached
     */
    private boolean fileEnd = false;

    /**
     * PreBufferedDataStreamElementFileReader constructor.
     *
     * @param properties Properties
     * @param file       File that have to be read
     */
    public PreBufferedDataStreamElementFileReader(Properties properties, File file) {
        this.properties = properties;
        this.file = file;
        this.buffer = null;
    }

    /**
     * Initializes all important variables and first time fills the buffer.
     *
     * @throws IOException Thrown in case of an IOException while creating the BufferedReader or filling the buffer.
     */
    public final void initialize() throws IOException {
        this.fileReader = new FileReader(this.file);
        this.bufferedReader = new BufferedReader(this.fileReader);

        this.factory = generateFactory();

        this.buffer = new LinkedList<>();

        this.bufferedReader.readLine(); // skip first line

        this.minBufferSize = PropertyReadHelper.readIntOrDie(this.properties, "fileReader.buffer.size.min");
        this.maxBufferSize = PropertyReadHelper.readIntOrDie(this.properties, "fileReader.buffer.size.max");

        fillBuffer();
    }

    /**
     * Generates the factory for generating data stream elements from lines.
     *
     * @return Factory
     */
    protected abstract DataStreamElementFromLineFactoryInterface<T> generateFactory();

    /**
     * Fills the buffer up the the maxBufferSize unless the end of the given file is reached.
     *
     * @throws IOException Thrown in case of an IOException while filling the buffer.
     */
    private void fillBuffer() throws IOException {
        if (!this.fileEnd && this.buffer.size() < this.maxBufferSize) {
            String newLine;

            while (this.buffer.size() < this.maxBufferSize) {
                newLine = this.bufferedReader.readLine();

                if (newLine == null) {
                    this.fileEnd = true;
                    return;
                } else {
                    try {
                        T newT = this.factory.generateFromLine(newLine);
                        this.buffer.addLast(newT);
                    } catch (AbstractImmutableDataStreamElement.CannotGenerateDataStreamElement e) {
                        logger.error("Caught exception during generating a data stream element from the line: ", e);
                    }
                }
            }
        }
    }

    /**
     * Polls a single data stream element from the buffer.
     * Refills the buffer if necessary, i.e., if the buffer size falls below minBufferSize and the end of the file is not reached yet.
     *
     * @return First data stream element from the buffer
     * @throws EmptyBufferException Thrown if the buffer is empty.
     * @throws IOException          Thrown in case of an IOException while filling the buffer.
     */
    protected final T pollElementFromBuffer() throws EmptyBufferException, IOException {
        if (this.buffer.size() > 0) {
            T res = this.buffer.pollFirst();

            if (!this.fileEnd && this.buffer.size() < this.minBufferSize) {
                fillBuffer();
            }
            return res;
        } else {
            throw new EmptyBufferException();
        }
    }

    /**
     * Adds a data stream element to the first position of the buffer
     *
     * @param element Element that has to be added
     */
    protected final void addToFirstPositionAtTheBuffer(T element) {
        this.buffer.addFirst(element);
    }

    /**
     * Closes the file reader.
     *
     * @throws IOException Thrown in case of an IOException while closing the BufferedReader and the FileReader.
     */
    public final void close() throws IOException {
        this.bufferedReader.close();
        this.fileReader.close();
    }

}
