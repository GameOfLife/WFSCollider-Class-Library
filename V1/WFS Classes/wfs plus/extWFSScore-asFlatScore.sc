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
	addToStartTime { |add = 0| startTime = startTime + add; }
	 
	flat { |startTimeOffset = 0, excludeMuted = false|
		if( this.isFolder )
			{ ^this.wfsSynth.flat( startTimeOffset + this.startTime, excludeMuted ); }
			{ ^[ this.copyNew.wfsSynth_( this.wfsSynth ).addToStartTime( startTimeOffset ) ]; };
		}
	
	}
	
+ WFSScore {

	flat { |startTimeOffset = 0, excludeMuted = false|
		if( excludeMuted )
			{ ^events.collect({ |event| 
				if( event.muted == false )
					{ event.flat( startTimeOffset ); }
					{ nil } }).flatten(1).select( _.notNil );  }
			{ ^events.collect({ |event| event.flat( startTimeOffset ); }).flatten(1); };

		}
	
	asFlatScore { |excludeMuted = false| 
		^this.class.new( *this.flat( 0, excludeMuted ) ).sort.name_( name )
			.clickTrackPath_( clickTrackPath ); }
	
	}