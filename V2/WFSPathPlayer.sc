WFSPathPlayer {
	
	*kr { |bufnum = 0, startIndex = 0, ratio = 1, loop = 0, reset = 1, 
			numDim = 2, doneAction = 0| // numDim: number of dimensions (x,y -> 2 dimensions)
		var phase, length, index, pos;
		
		// buffer format:
		
		// ch 1: times
		// ch 2-5: spline part 1 values for dimension 1
		// ch 6-10: spline part 1 values for dimension 2
		// etc..
		
		length = if( loop.booleanValue, inf, (BufFrames.kr( bufnum ) - startIndex.floor) );
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
		
		length = if( loop.booleanValue, inf, (BufFrames.kr( bufnum ) - startIndex.floor) );
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

+ WFSPath2 {
	asBufferArray {
		var controls;
		controls = this.controls;
		^positions.collect({ |pos, index|
			([times.clipAt( index )] ++ positions.wrapAt([ index, index+1])
				.splineIntPart1( *(controls[index]) ).collect(_.asArray).flop).flat;
		}).flat;
	}
	
	asBuffer { |server, bufnum, action, load = false|
		var array, buf;
		array = this.asBufferArray;
		if( load ) {
			^Buffer.loadCollection( server, array, 9, action ); 
		} {
			^Buffer.sendCollection( server, array, 9, 0, action ); 
		};
	}
}

