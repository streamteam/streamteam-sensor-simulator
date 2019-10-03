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

#https://www.cyberciti.biz/faq/bash-for-loop/
for (( ; ; ))
do
	./startSensorSimulator.sh $line $match &
	sleep 46m
	echo "Slept for 46 minutes"
	./stopSensorSimulatorsOnNode.sh $line
	echo "Stopped SensorSimulator."
	sleep 10s
done
