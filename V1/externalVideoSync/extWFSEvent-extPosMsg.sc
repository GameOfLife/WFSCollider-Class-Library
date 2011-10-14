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

+ WFSEvent {

	extStartMsg {  |id=0|
		^[ "/wfs", "/start", startTime, "/type", wfsSynth.intType, "/id", id ];
		}
		
	extStopMsg {  |id=0, time|
		^[ "/wfs", "/stop", time ? this.endTime, "/id", id  ];
		}
		
	extPosMsg {  |time=0, id=0, first = false|
		var pos;
		if( [\static, \plane, \index ].includes( wfsSynth.intType ) && { first == false })
			{ ^nil }
			{    
			case { wfsSynth.wfsPath.class == WFSPath; }
				{ pos = wfsSynth.wfsPath.atTime2( time - startTime, 'hermite', false );
					^[ "/wfs","/point", pos.x, pos.y, time, "/id", id  ]; }
				
				{ wfsSynth.wfsPath.class == WFSPoint; }
				{^[ "/wfs", "/point", wfsSynth.wfsPath.x, wfsSynth.wfsPath.y, time, "/id", id]; }
				
				{ wfsSynth.wfsPath.class == WFSPlane; }
				{^[ "/wfs", "/plane", wfsSynth.wfsPath.angle, wfsSynth.wfsPath.distance,
					 time, "/id", id  ]; }
				
				{ wfsSynth.wfsPath.isNumber; }
				{^[ "/wfs", "/index", wfsSynth.wfsPath, time, "/id", id  ]; }
					
				{ true }
				{ ^nil }; 
			};
		}
	
	}
	
+ WFSScore {
	eventsAtTime { |time = 0|
		^events.select({ |event| time.inclusivelyBetween( event.startTime, event.endTime ); });
		}
	}