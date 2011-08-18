WFSArrayPanSynth {
	
	/*
	generates and maintains the SynthDefs needed for array panners.
	Normally an array panner synthdef is preceeded by a pre panner, but static sources
	might not need one of those (or only for the env and gain)
	*/
	
	classvar <>synthDefs;
	classvar <>minSize = 8, <>maxSize = 64, <>division = 8; // if we get > 64 we might want to combine multiple
	classvar <>types, <>modes, <>intTypes;
	
	*initClass {
		types = [ \n, \f, \u, \p ]; // normal, focused, uni (= normal and focused), plane
		modes = [ \s, \d ];  // static, dynamic
		intTypes = [ \n, \l, \c ];  // non-int, linear, cubic
	}
	
	*allSizes { ^( minSize, minSize + division .. maxSize ) }
	
	*getDefName { |size = 8, type = \u, mode = \s, int = \n|
		
		#type, mode, int = [ type, mode, int ].collect({ |item|
			item.asString[0].toLower;
		});
		
		// example of synthdef name:
		// 'wfsa_fdl_32' : focused dynamic linear point, 32 speakers
		// 'wfsa_psn_40' : static non-interpolating plane, 40 speakers
		
		^["wfsa", [type, mode, int].join(""), size ].join("_");
	}
	
	*generateDef { |size = 8, type = \uni, mode = \static, int = \n|
		var conf;
		
		#type, mode, int = [ type, mode, int ].collect({ |item, i|
			var out;
			out = item.asString[0].toLower.asSymbol;
			if( [ types, modes, intTypes ][i].includes( out ).not ) {
				"WFSArrayPanSynth.generateDef - nonexistent %: %"
					.format( [\type, \mode, \int][i], item )
					.warn;
			};
			out;
		});
		
		^SynthDef( this.getDefName(size, type, mode, int), {
			
			// synth args:
			var in_bus = 0, arrayConf, outOffset = 0, addDelay = 0;
			var point = 0@0, amp = 1, dbRollOff = -9;
			
			// local variables
			var gain = -40.dbamp; // hard-wired for now
			var panner, input;
			
			// always dynamic:
			in_bus = \in_bus.kr( in_bus ); 
			
			// always static
			arrayConf = \arrayConf.ir( [ 5, -0.5pi, 0, 0.164 ] );
			outOffset = \outOffset.ir( outOffset );
			addDelay = \addDelay.ir( addDelay );
			
			if( mode === \d ) {
				point = In.kr( \point_bus.kr([0,1]) ).asPoint;
				amp = In.kr( \amp_bus.kr(2) );
				dbRollOff = \dbRollOff.kr( -9 );
			} {
				point = \point.ir([0,0]).asPoint;
				amp = \amp.kr(amp);
				dbRollOff = \dbRollOff.ir( dbRollOff );
			};
			
			input = PrivateIn.ar( in_bus ) * gain;
			
			if( type === \p ) {
				panner = WFSArrayPanPlane( size, *arrayConf ).addDelay_( addDelay ); // dbRollOff not used
			} {
				panner = WFSArrayPan( size, *arrayConf )
					.addDelay_( addDelay )
					.dbRollOff_( dbRollOff )
					.focus_( switch( type, \f, { true }, \n, { false }, { nil } ) );
			};
			
			Out.ar( outOffset, panner.ar( input, point, int, amp ) ); 
			
			
		});
	}
	
	*generateAllDefs { |action, estimatedTime = 27, dir| // and write to disk
		
		// this takes about 30 seconds in normal settings
		// can be stopped via cmd-.
		
		var all, waitTime, defs;
		dir = dir ? SynthDef.synthDefDir;
		all = #[ // these are the all types we'll probably need
			[ uni, static, n ],    // use this for any static
			[ normal, static, n ], // use this for normal static
			[ uni, dynamic, n ],
			[ uni, dynamic, l ], // perhaps we should add a fadeout or narrow-down at crosspoint
			[ uni, dynamic, c ], // for these two
			[ focus, dynamic, l ],
			[ normal, dynamic, l ],
			[ focus, dynamic, c ],
			[ normal, dynamic, c ],
			[ plane, static, n ],
			[ plane, dynamic, n ],
			[ plane, dynamic, l ],
			[ plane, dynamic, c ] 
		];
		waitTime = estimatedTime / all.size;
		
		// now we generate them:
		{	
			var started;
			started = Main.elapsedTime;
			"started generating WFSArrayPanSynth synthdefs".postln;
			" this may take % seconds or more\n".postf( estimatedTime );
			defs = all.collect({ |item|
				var out = WFSArrayPanSynth.allSizes.collect({ |size|
					WFSArrayPanSynth.generateDef(size, *item ).writeDefFile( SynthDef.synthDefDir );
				});
				waitTime.wait;
				"  WFSArrayPanSynth synthdefs for % ready\n".postf( item.join("_") );
				out;
			});
			"done generating WFSArrayPanSynth synthdefs in %s\n".postf( (Main.elapsedTime - started).round(0.001) );
			action.value( defs );
		}.fork();
	}
	
	
}