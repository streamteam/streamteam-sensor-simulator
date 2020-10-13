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

node=`cat sensorSimulatorNode.txt`

#http://stackoverflow.com/questions/59895/getting-the-source-directory-of-a-bash-script-from-within
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $DIR

cd ..

mvn clean
mvn package

cd ..
rm streamteam-sensor-simulator.tar.gz
tar -czf streamteam-sensor-simulator.tar.gz streamteam-sensor-simulator/

for node in `cat $DIR/sensorSimulatorNode.txt`; do
	echo "=== Deploy on $node ==="
	ssh -i ~/.ssh/lukasPMAAS ubuntu@$node "rm -Rf streamteam-sensor-simulator"
	scp -i ~/.ssh/lukasPMAAS ./streamteam-sensor-simulator.tar.gz ubuntu@$node:streamteam-sensor-simulator.tar.gz
	ssh -i ~/.ssh/lukasPMAAS ubuntu@$node "tar -xzf streamteam-sensor-simulator.tar.gz"
	ssh -i ~/.ssh/lukasPMAAS ubuntu@$node "rm streamteam-sensor-simulator.tar.gz"
done

rm streamteam-sensor-simulator.tar.gz
