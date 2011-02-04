WFSPathPlayer {
	
	*kr { |bufnum = 0, startIndex = 0, ratio = 1, loop = 0, reset = 1, 
			numChannels = 2, doneAction = 0|
		var phase, length, index, pos;
		
		// buffer format:
		
		// ch 1: times
		// ch 2-5: spline part 1 values for channel 1
		// ch 6-10: spline part 1 values for channel 2
		// etc..
		
		length = if( loop.booleanValue, inf, (NumFrames.kr( bufnum ) - startIndex.floor) - 1 );
		index = Dseries( startIndex.floor, 1, length );
		phase = DemandEnvGen.kr( index + Dseq([startIndex.frac, Dseq([0],length - 1)], 1),
				Dbufrd( bufnum, index ) * Dseq([1-startIndex.frac, Dseq([1],length - 1)], 1),
				timeScale: 1/ratio,
				reset: reset,
				doneAction: doneAction );
		pos = BufRd.kr( (numChannels * 4) + 1, bufnum, phase, 1, 1 );
		
		^numChannels.collect({ |i| pos[ (1..4) + (i*4) ].splineIntPart2; });
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
