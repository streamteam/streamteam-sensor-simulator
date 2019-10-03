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

import java.util.Properties;

/**
 * TimeProvider types.
 */
public enum TimeProviderType {
    /**
     * LocalMachineTimeProvider.
     */
    LOCAL {
        @Override
        public TimeProviderInterface getTimeProvider(Properties properties) {
            return new LocalMachineTimeProvider();
        }
    };

    /**
     * Returns a TimeProviderInterface instance.
     *
     * @param properties Properties
     * @return TimeProviderInterface instance
     */
    public abstract TimeProviderInterface getTimeProvider(Properties properties);
}
