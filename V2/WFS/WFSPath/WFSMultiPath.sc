/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2011 Wouter Snoei.

    GameOfLife WFSCollider software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife WFSCollider is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife WFSCollider.  If not, see <http://www.gnu.org/licenses/>.
*/

WFSMultiPath {
	var <>paths;
	var <>fillTime = 0.1;
	
	prPathIndex { |index = 0|
		var sizes, pathIndex;
		sizes = paths.collect({ |item| item.positions.size }).integrate;
		pathIndex = sizes.detectIndex( index < _ );
		^[ pathIndex, index - (sizes[ pathIndex - 1 ] ? 0) ]
	}
	
	at { |index| 
		^paths.at(index) 
	}
	
	positions {
		^paths.collect(_.positions).flatten(1);
	}
	
	positions_ { |positions|
		positions.clumps( paths.collect(_.positions.size) ).do({ |item, i|
			paths[i].positions = item;
		});	
	}
	
	times {
		var times = [];
		paths.do({ |pth|
			times ++ pth.times ++ [fillTime]
		});
		^times;
	}

	forceTimes {
		// todo
	}
	
	length {
		^paths.collect(_.length).maxItem;
	}
	
	asRect {
		^if( paths.size > 0 ) { 
			paths.collect(_.asRect).reduce(\union);
		} {
			Rect(0,0,0,0);
		}
	}
	
}