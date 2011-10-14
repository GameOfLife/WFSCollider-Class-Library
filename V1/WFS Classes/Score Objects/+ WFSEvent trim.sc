/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2010 Miguel Negr‹o.

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

//trims, resizing, etc

+ WFSEvent {
	
	trimEnd { |newEnd, removeFade = false|
		
		var delta = newEnd - startTime;
		
		if( delta > 0) {
			
			wfsSynth.dur = delta;
			if(removeFade){
				wfsSynth.fadeOutTime = 0
			};			
			wfsSynth.clipFadeOut;					
			if( wfsSynth.wfsPath.class == WFSPath ) { 
				wfsSynth.adjustWFSPathToDuration;
			}
		}		
	}

	trimStart{ |newStart,removeFade = false|
		var delta1,delta2;
		delta1 = newStart - startTime;
		delta2 = wfsSynth.startFrame + (44100 * delta1);
		if(newStart < this.endTime) {
			
			if(delta1 > 0) {
				//making event shorter
				startTime = newStart;
				wfsSynth.dur = wfsSynth.dur - delta1;
				wfsSynth.startFrame = delta2;
				if(removeFade){
					wfsSynth.fadeInTime = 0
				};
				wfsSynth.clipFadeIn;
				if( wfsSynth.wfsPath.class == WFSPath ) { 
				 	wfsSynth.wfsPath.length_(wfsSynth.dur);
				}
				
			} {	//making event bigger
				if(delta2 >= 0) {
					startTime = newStart;
					wfsSynth.dur = wfsSynth.dur - delta1;
					wfsSynth.startFrame = delta2;	
					if(removeFade){
						wfsSynth.fadeInTime = 0
					};
					wfsSynth.clipFadeOut;
					if( wfsSynth.wfsPath.class == WFSPath ) { 
						wfsSynth.adjustWFSPathToDuration;
					}
				}
			}
					
		}
	}
	
	/*
	resizeFront { |newStartTime|
		var oldEndTime = this.endTime;
		startTime = newStartTime
			.min( oldEndTime - 2 )
			.max( oldEndTime - wfsSynth.soundFileDur );
		this.dur = oldEndTime - newStartTime;
		if( wfsSynth.wfsPath.class == WFSPath ) { 
			wfsSynth.wfsPath.length_(this.dur);
		};
		wfsSynth.clipFadeIn;
	}
	
	resizeBack { |newEndTime|
		("resize back "++newEndTime).postln;
		this.dur = newEndTime - startTime;
		if( wfsSynth.wfsPath.class == WFSPath ) { 
			wfsSynth.wfsPath.length_(this.dur);
		};
		wfsSynth.clipFadeOut;
	
	}
	*/
}

+ WFSSynth {				
					
	clipFadeIn{
		this.fadeInTime = this.fadeInTime.min(dur);			this.fadeOutTime = this.fadeOutTime.min(dur - this.fadeInTime);
	}
		
	clipFadeOut{
		var delta;
		this.fadeOutTime = this.fadeOutTime.min(dur);			this.fadeInTime = this.fadeInTime.min(dur - this.fadeOutTime);
	}
	
	adjustWFSPathToDuration {
		wfsPath.length_(dur);		
	}
}


+ ScaledUserView {
	
	doScale { |point|
		^point*this.scale*this.drawBounds.extent / this.fromBounds.extent	
	}
	
	doReverseScale{ |point|
		^point / 	(this.drawBounds.extent / this.fromBounds.extent * this.scale )
	}
}

/*
WFSMouseEventsManager {

	var <> moveFlag = false, resizeFrontFlag = false, resizeBackFlag = false, moveOrigin, movingRects, resizeOrigin, resizingRect;
	var resizeOldStartTime, resizeOldEndTime, selectedRects;
	
}
*/

