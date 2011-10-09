WFSPathPlayer {
	
	*kr { |bufnum = 0, startIndex = 0, ratio = 1, loop = 0, reset = 1, 
			numDim = 2, doneAction = 0| // numDim: number of dimensions (x,y -> 2 dimensions)
		var phase, length, index, pos;
		
		// buffer format:
		
		// ch 1: times
		// ch 2-5: spline part 1 values for dimension 1
		// ch 6-10: spline part 1 values for dimension 2
		// etc..
		
		length = Select.kr( loop, [ (BufFrames.kr( bufnum ) - startIndex.floor), inf ] );
		index = Dstutter( 2, Dseries( startIndex.floor, 1, length ));
		phase = DemandEnvGen.kr( 
				index + Dseq([startIndex.frac, Dseq([0],length - 1)], 1),
				Dbufrd( bufnum, index ) * Dseq([1-startIndex.frac, Dseq([1],length - 1)], 1),
				timeScale: 1/ratio,
				reset: reset,
				doneAction: doneAction );
		pos = BufRd.kr( (numDim * 4) + 1, bufnum, phase, 1, 1 );
		^numDim.collect({ |i| pos[ (1..4) + (i*4) ].splineIntPart2( phase.wrap(0,1) ); });
	}
	
	*ar { |bufnum = 0, startIndex = 0, ratio = 1, loop = 0, reset = 1, 
			numDim = 2, doneAction = 0|
		var phase, length, index, pos;
		
		length = Select.kr( loop, [ (BufFrames.kr( bufnum ) - startIndex.floor), inf ] );
		index = Dstutter( 2, Dseries( startIndex.floor, 1, length ));
		phase = DemandEnvGen.ar( 
				index + Dseq([startIndex.frac, Dseq([0],length - 1)], 1),
				Dbufrd( bufnum, index ) * Dseq([1-startIndex.frac, Dseq([1],length - 1)], 1),
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
	
	*ar { |key, trigger = 1, startPos = 0, doneAction = 0|
		^WFSPathPlayer.ar( *this.getArgs( key, trigger, startPos ) ++ [ doneAction ] );
	}
	
	*kr { |key, trigger = 1, startPos = 0, doneAction = 0|
		^WFSPathPlayer.kr( *this.getArgs( key, trigger, startPos ) ++ [ doneAction ] );
	}
	
}

