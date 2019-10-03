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

/**
 * Interface for factories which can generate a data stream element from a single line (string).
 *
 * @param <T> Implementation of AbstractImmutableDataStreamElement
 */
public interface DataStreamElementFromLineFactoryInterface<T extends AbstractImmutableDataStreamElement> {

    /**
     * Generates a data stream element (AbstractImmutableDataStreamElement implementation) given a single line (string).
     *
     * @param line Line (string) that specifies the data stream element
     * @return Data stream element that is generated using the given line
     * @throws AbstractImmutableDataStreamElement.CannotGenerateDataStreamElement Thrown if the data stream element could not be generated
     */
    T generateFromLine(String line) throws AbstractImmutableDataStreamElement.CannotGenerateDataStreamElement;

}
