/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2010 Wouter Snoei.

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

// only one of these per synthdef!! 

// outputs a moving WFSPoint


PlayWFSPath {

	*kr { |bufXYZ, bufT, intType = 4, startIndex = 0, ratio = 1, trig = 0| 
		//cubic path interpolation
		var pos, phase;
		phase = Sweep.kr(trig, LocalIn.kr(1) ) + startIndex;  
		LocalOut.kr( ratio / BufRd.kr(1, bufT, phase, 0, 1));
		^WFSPoint( *BufRd.kr(3, bufXYZ, phase, 0, intType) ).z_(0);
		 		}
		 		
	*kr2D { |bufXY, bufT, intType = 4, z=0| //cubic path interpolation; 2D
		var pos, phase, xyz;
		phase = Sweep.kr(0, LocalIn.kr(1));  
		LocalOut.kr(1 / BufRd.kr(1, bufT, phase, 0, 1));
		xyz = BufRd.kr(2, bufXY, phase, 0, intType) ++ [z];
		^WFSPoint( *xyz );
		 		}
		 		
	*kr2 { |bufXYZ, bufT, intType = 4| //cubic path interpolation
		var pos, phase;
		phase = Sweep.kr(0, 1);  
		//LocalOut.kr(1 / BufRd.kr(1, bufT, phase, 0, 1));
		^WFSPoint( *BufRd.kr(3, bufXYZ, phase, 0, intType) );
		 		}
		
	*ar { |bufXYZ, bufT, intType = 4| 
		var pos, phase;
		phase = Sweep.ar(0, LocalIn.ar(1));  
		LocalOut.ar(1 / BufRd.ar(1, bufT, phase, 0, 1));
		^WFSPoint.new( *BufRd.ar(3, bufXYZ, phase, 0, intType) ); 		}
		
	}