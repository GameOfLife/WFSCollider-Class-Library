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


+ WFSPath2 {
	asBufferArray {
		var controls;
		controls = this.controls;
		^positions.collect({ |pos, index|
			([times.clipAt( index )] ++ positions.wrapAt([ index, index+1])
				.splineIntPart1( *(controls[index]) ).collect(_.asArray).flop).flat;
		}).flat;
	}
	
	*fromBufferArray { |bufferArray, name|
		var times, positions, p2, c1, c2, controls;
		var path, intType = 'cubic', clipMode = 'clip';
		
		bufferArray = bufferArray.clump(9);
		
		// get the times and controls from array
		
		#times, positions, p2, c1, c2 = bufferArray.collect({ |vals|
			var time, y1, y2, x1, x2, controls;
			time = vals[0];
			#y1, y2, x1, x2 = vals[1..8].clump(4).collect({ |item|
				this.reverseSplineIntPart1( *item );
			}).flop.collect(_.asPoint);
			
			[ time, y1, y2, x1, x2 ];
		}).flop;
		
		path = this.new( positions, times, name );
		
		// guess intType and clipMode by checking against all possible controls
		switch ( positions.last.round(1e-12),
			positions.last.round(1e-12), { clipMode = 'clip' },
			positions.first.round(1e-12), { clipMode = 'wrap' },
			positions[ positions.size-2 ].round(1e-12), { clipMode = 'fold' }
		);
		
		controls = [c1, c2].flop.round( 1e-12 );
		
		block { |break|
			([ clipMode ] ++ (#[ clip, wrap, fold ].select(_ != clipMode))).do({ |cm|
				#[ cubic, bspline, linear ].do({ |it|
					if( path.generateAutoControls( it, cm ).round(1e-12) == controls ) {
						clipMode = cm;
						intType = it;
						break.value;
					};
				});
			});
			"WFSPath2.fromBufferArray: could not guess intType and/or clipMode from raw array"
				.warn;
		};
		
		path.intType = intType;
		path.clipMode = clipMode;
		
		^path;	
	}
	
	*reverseSplineIntPart1 { |y1, c1, c2, c3|
		var x1, x2, y2;
		x1 = (c1 / 3) + y1;
		x2 = (c2 / 3) + (x1*2) - y1;
		y2 = (c3 + y1) + ((x2 - x1) * 3);
		^[ y1, y2, x1, x2 ];
	}
	
	asBuffer { |server, bufnum, action, load = false|
		// ** NOT USED BY WFSPathBuffer ** //
		var array, buf;
		array = this.asBufferArray;
		if( load ) {
			^Buffer.loadCollection( server, array, 9, action ); 
		} {
			^Buffer.sendCollection( server, array, 9, 0, action ); 
		};
	}

}

