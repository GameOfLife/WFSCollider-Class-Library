/*
(
x = MetaU(\param_beq,[\numChannels,1],[\eq_controls,EQSpec().default]);
y = Udef(\noise,{ UOut.ar(0, WhiteNoise.ar) });
z = UChain(\noise,x,\output);
z.prepareAndStart(s);
)

x.set(\eq_controls,EQArg([ [ 100, 0, 1 ], [ 250, 0, 1 ], [ 1000, 0, 1 ], [ 3500, 0, 1 ], [ 10000, -24, 1 ] ]))
*/
x = MetaUdef(\param_beq,{ |numChannels = 1|
	
	var fromControl= { |controls|
		controls.clump(3).collect({ |item|
			[ (item[0] + 1000.cpsmidi).midicps, item[1], 10**item[2]]
		});
	};
	
	var synthGraph = { |input|
		var frdb;
		frdb = fromControl.value( Control.names([\eq_controls]).kr( 0!15 ) );
				
		input = BLowShelf.ar( input, *frdb[0][[0,2,1]].lag(0.1) );
		input = BPeakEQ.ar( input, *frdb[1][[0,2,1]].lag(0.1));
		input = BPeakEQ.ar( input, *frdb[2][[0,2,1]].lag(0.1));
		input = BPeakEQ.ar( input, *frdb[3][[0,2,1]].lag(0.1));
		input = BHiShelf.ar( input, *frdb[4][[0,2,1]].lag(0.1));
		
		input;
	};
			
	{ | gate = 1, fadeTime = 0.05, doneAction = 2|

		var frdb, input, env;
		input = UIn.ar(0, numChannels);
		input = synthGraph.value( input );
		UOut.ar(0, input)
	}	 
},[[\numChannels,1,PositiveIntegerSpec()]],
[
    [\eq_controls,nil,EQSpec.new],
    ArgSpec(\fadeTime,0.1,PositiveRealSpec(), true),
    ArgSpec(\doneAction,2,PositiveIntegerSpec(), true),
    ArgSpec(\gate,1,BoolSpec(), true)
])