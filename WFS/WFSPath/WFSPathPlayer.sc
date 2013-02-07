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

WFSPathPlayer {
	
	*kr { |bufnum = 0, startIndex = 0, ratio = 1, loop = 0, reset = 1, 
			numDim = 2, waitTime = 0, doneAction = 0| // numDim: number of dimensions (x,y -> 2 dimensions)
		var phase, length, index, pos;
		
		// buffer format:
		
		// ch 1: times
		// ch 2-5: spline part 1 values for dimension 1
		// ch 6-10: spline part 1 values for dimension 2
		// etc..
		
		length = Select.kr( loop, [ (BufFrames.kr( bufnum ) - startIndex.floor), inf ] );
		phase = DemandEnvGen.kr( 
				Dseq([ startIndex, startIndex, Dseries( startIndex.floor + 1, 1, length-1 )],1),
				Dseq([ waitTime * ratio, Dbufrd( bufnum, Dseries( startIndex.floor, 1, length ) ) 
					* Dseq([1-startIndex.frac, Dseq([1],length - 1)], 1)
				],1),
				timeScale: 1/ratio,
				reset: reset,
				doneAction: doneAction );
		pos = BufRd.kr( (numDim * 4) + 1, bufnum, phase, 1, 1 );
		^numDim.collect({ |i| pos[ (1..4) + (i*4) ].splineIntPart2( phase.wrap(0,1) ); });
	}
	
	*ar { |bufnum = 0, startIndex = 0, ratio = 1, loop = 0, reset = 1, 
			numDim = 2, waitTime = 0, doneAction = 0|
		var phase, length, index, pos;
		
		length = Select.kr( loop, [ (BufFrames.kr( bufnum ) - startIndex.floor), inf ] );
		index = Dstutter( 2, Dseries( startIndex.floor, 1, length ));
		phase = DemandEnvGen.ar( 
				Dseq([ startIndex, startIndex, Dseries( startIndex.floor + 1, 1, length-1 )],1),
				Dseq([ waitTime * ratio, Dbufrd( bufnum, Dseries( startIndex.floor, 1, length ) ) 
					* Dseq([1-startIndex.frac, Dseq([1],length - 1)], 1)
				],1),
				timeScale: 1/ratio,
				reset: reset,
				doneAction: doneAction );
		pos = BufRd.ar( (numDim * 4) + 1, bufnum, phase, 1, 1 );
		^numDim.collect({ |i| pos[ (1..4) + (i*4) ].splineIntPart2( phase.wrap(0,1) ); });
	}
	
	*krOld { |bufXYZ, bufT, intType = 4, startIndex = 0, ratio = 1, reset = 1, doneAction = 0| 
		//cubic path interpolation
		var pos, phase;
		
		phase = DemandEnvGen.kr( 
				Dseries( startIndex.floor, 1, inf ) + Dseq([startIndex.frac, Dseq([0],inf)], 1),
				Dbufrd( bufT, Dseries(  startIndex.floor, 1, inf ) ) 
					* Dseq([1-startIndex.frac, Dseq([1],inf)], 1),
				timeScale: 1/ratio,
				reset: reset,
				doneAction: doneAction
				);	
						
		^WFSPoint( *BufRd.kr(3, bufXYZ, phase, 0, intType) );
		 }
}

WFSPathBufferPlayer { // use this inside an Udef
	
	*getArgs { |key, trigger = 1, startPos = 0| 
		var bufnum, rate, loop, startFrame;
		key = key ? 'wfsPath';
		#bufnum, startFrame, rate, loop = key.asSymbol.kr( [ 0, 0, 1, 0 ] );
		startFrame = startFrame + startPos;
		^[ bufnum, startFrame, rate, loop, trigger, 2 ];
	}
	
	*ar { |key, trigger = 1, startPos = 0, waitTime = 0, doneAction = 0|
		^WFSPathPlayer.ar( *this.getArgs( key, trigger, startPos ) ++ [ waitTime, doneAction ] );
	}
	
	*kr { |key, trigger = 1, startPos = 0, waitTime = 0, doneAction = 0|
		^WFSPathPlayer.kr( *this.getArgs( key, trigger, startPos ) ++ [ waitTime, doneAction ] );
	}
	
}

