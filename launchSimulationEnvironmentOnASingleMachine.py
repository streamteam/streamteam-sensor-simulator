#!/usr/bin/python

#
# StreamTeam
# Copyright (C) 2019  University of Basel
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

import platform
import re
import subprocess
import sys
import time
import uuid

if len(sys.argv) != 2:
    sys.exit("Required parameters: <match>")

match = sys.argv[1]

p = re.compile('CYGWIN')
m = p.match(platform.system())
if m:
    separator = ';'
else:
    separator = ':'

commandPrefix = ""
commandSuffix = "&"

matchConfigFile = "./sensorData/" + match + "/config.properties"

pathToSensorDataFiles = "./sensorData/" + match + "/"

matchId = uuid.uuid4().int % 1000000

execfile("./sensorData/" + match + "/sids.py")

print "Calculate starting timestamp parameter for sensor simulators..."
curTimeInS = time.time()
simulationStartTimeInS = curTimeInS + 40  # start simulation 40 seconds in the future
simulationStartTimeInMs = long(simulationStartTimeInS * 1000)
simulationStartTimeInMsString = str(simulationStartTimeInMs)
print "Starting timestamp in ms: " + simulationStartTimeInMsString

print "Start Sensor Simulators..."

isMatchAnnouncer = "true"

for curSid in sids:
    print "Start Sensor Simulator for sensor " + curSid

    logFileNameCommandPart = "-DlogFileName=SensorSimulator_" + str(matchId) + "_" + curSid.replace("/", "_") + "_local"
    jarCommandPart = "-jar ./target/streamteam-sensor-simulator-1.0.1-jar-with-dependencies.jar"
    curPathToSensorDataFile = pathToSensorDataFiles + curSid + ".csv"
    argsCommandPart = logFileNameCommandPart + " " + jarCommandPart + " " + matchConfigFile + " " + str(matchId) + " " + curPathToSensorDataFile + " " + simulationStartTimeInMsString + " " + isMatchAnnouncer

    curCmd = "java " + argsCommandPart

    print curCmd
    subprocess.call(commandPrefix + curCmd + commandSuffix, shell=True)

    isMatchAnnouncer = "false"

print "Simulation environment started"
