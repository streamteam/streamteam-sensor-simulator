#!/bin/bash

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

#http://stackoverflow.com/questions/18568706/check-number-of-arguments-passed-to-a-bash-script
if [ "$#" -ne 1 ]; then
	echo "Expected parameters: <line>"
	echo "Used default values."
	line=1
else
	line=$1
fi

echo "Line: "$line

#http://stackoverflow.com/questions/59895/getting-the-source-directory-of-a-bash-script-from-within
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $DIR

#https://stackoverflow.com/questions/15632691/fastest-way-to-print-a-single-line-in-a-file
node="$( head -n $line sensorSimulatorNode.txt | tail -1)"

echo "=== Stop all Sensor Simulators via ssh on $node ==="
ssh -i ~/.ssh/lukasPMAAS ubuntu@$node ./streamteam-sensor-simulator/kill.sh
