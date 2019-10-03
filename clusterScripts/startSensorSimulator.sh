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

die() {
	echo >&2 "$@"
	exit 1
}
[ "$#" -ge 2 ] || die "requires at least two arguments (line, match), $# provided"

line=$1
match=$2

echo "Line: "$line
echo "Match: "$match

#http://stackoverflow.com/questions/59895/getting-the-source-directory-of-a-bash-script-from-within
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $DIR

#https://stackoverflow.com/questions/15632691/fastest-way-to-print-a-single-line-in-a-file
node="$( head -n $line sensorSimulatorNode.txt | tail -1)"

echo "=== Start Sensor Simulator via ssh on $node ==="
ssh -i ~/.ssh/lukasPMAAS ubuntu@$node "cd streamteam-sensor-simulator; ./launchSimulationEnvironmentOnASingleMachine.py "$match
