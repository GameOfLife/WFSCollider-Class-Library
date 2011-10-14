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

	performAll { |whatToPerform ... args|
		if( this.isFolder )
			{ ^wfsSynth.events.collect({ |item| item.perform( whatToPerform, *args ) }) }
			{ ^wfsSynth.perform( whatToPerform, *args ) ; }
		}
		
		
	position { ^this.performAll( \position ); }
	position_ { |newPos, changeDur = true| wfsSynth.position_( newPos, changeDur ); }
	
	filePath { ^this.performAll( \filePath ); }
	filePath_ { |newFilePath| wfsSynth.filePath = newFilePath; }
	
	pbRate { ^this.performAll( \pbRate ); }
	pbRate_ { |newPbRate| wfsSynth.pbRate = newPbRate; }
	
	startFrame { ^this.performAll( \startFrame ); }
	args { ^this.performAll( \args ); }
	level { ^this.performAll( \level ); }  
	
	events { if( this.isFolder ) { ^wfsSynth.events } { ^[] } }
	at { |index=0| if( this.isFolder ) { ^wfsSynth.at(index)  } { ^nil } }
	collect { |func|  if( this.isFolder ) {^wfsSynth.collect( func ); } { ^[] }  }
	do { |func| if( this.isFolder ) { wfsSynth.do( func ); } { func.value( this, 0 ) } }
	first { if( this.isFolder ) { ^wfsSynth.first } { ^this } }
	last { if( this.isFolder ) { ^wfsSynth.last } { ^this } }
	
	fadeInTime { ^this.performAll( \fadeInTime ) }
	fadeOutTime { ^this.performAll( \fadeOutTime ) }
	
	}
	
+ WFSSynth {
	position { ^wfsPath }
	position_ { |newPos, changeDur = true| this.wfsPath = newPos; }
	}
	