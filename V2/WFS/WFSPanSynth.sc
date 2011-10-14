WFSArrayPanSynthDefs {
	
	/*
	generates and maintains the SynthDefs needed for array panners.
	Normally an array panner synthdef is preceeded by a pre panner, but static sources
	might not need one of those (or only for the env and gain)
	*/
	
	classvar <>synthDefs;
	classvar <>minSize = 8, <>maxSize = 64, <>division = 8; 
		// if we get > 64 we might want to combine multiple
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
			var arrayConf, outOffset = 0, addDelay = 0;
			var point = 0@0, amp = 1, arrayRollOff = -9, arrayLimit = 1;
			
			// local variables
			var gain = -10.dbamp; // hard-wired for now
			var panner, input;
			
			// always static
			arrayConf = \arrayConf.ir( [ size, 5, 0.5pi, 0, 0.164 ] ); // size is fixed in def
			outOffset = \outOffset.ir( outOffset );
			addDelay = \addDelay.ir( addDelay );
			
			// depending on mode
			if( mode === \d ) {
				point = UIn.kr(0,2).asPoint;
				amp = In.kr( \levelBus.kr(2000) );
				if( type != \p ) { // only for points, not planes
					arrayRollOff = \arrayDbRollOff.kr( arrayRollOff );
					arrayLimit = \arrayLimit.kr( arrayLimit );
				};			
			} {
				point = \point.ir([0,0]).asPoint;
				amp = \amp.kr(amp);
				if( type != \p ) {
					arrayRollOff = \arrayRollOff.ir( arrayRollOff );
					arrayLimit = \arrayLimit.ir( arrayLimit );
				};
			};
			
			input = UIn.ar(0, 1) * gain;
			
			if( type === \p ) {
				panner = WFSArrayPanPlane( size, *arrayConf[1..] ).addDelay_( addDelay );
			} {
				panner = WFSArrayPan( size, *arrayConf[1..] )
					.addDelay_( addDelay )
					.dbRollOff_( arrayRollOff )
					.limit_( arrayLimit )
					.focus_( switch( type, \f, { true }, \n, { false }, { nil } ) );
			};
			
			Out.ar( outOffset, panner.ar( input, point, int, amp ) ); 
			
			
		});
	}
	
	*generateAll { |action, estimatedTime = 27, dir| // and write to disk
		
		// this takes about 30 seconds in normal settings
		// can be stopped via cmd-.
		
		var all, waitTime;
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
			synthDefs = all.collect({ |item|
				var out = this.allSizes.collect({ |size|
					this.generateDef(size, *item )
						.writeDefFile( dir );
				});
				waitTime.wait;
				"  WFSArrayPanSynth synthdefs for % ready\n".postf( item.join("_") );
				out;
			});
			"done generating WFSArrayPanSynth synthdefs in %s\n"
				.postf( (Main.elapsedTime - started).round(0.001) );
			action.value( synthDefs );
		}.fork();
	}
}

WFSPrePanSynthDefs {
	
	/*
	generates and maintains the SynthDefs needed for pre panners.
	Normally a pre panner comes before one or more array panners.
	it provides them with a delayed input (large global delay) with 
	global amplitude rolloff applied on the source. Crossfade levels 
	are also provided for dynamic sources (static sources take them
	from the lang), and array panners can be paused/unpaused from here.
	*/
	
	// we have an UEnv and a WFSLevelBus in here as well
	// should we throw in global eqhere too?
	
	classvar <>synthDefs;
	classvar <>maxArrays = 4; // should do for now
	
	classvar <>crossfadeModes, <>modesThatNeedArrays;
	
	
	*initClass {
		crossfadeModes = [ \d, \u, \p, \n ];  // dual, uni, plane, none (none: static sources)
		modesThatNeedArrays = [ \d, \u, \p ];
	}
	
	*getDefName { |numArrays = 1, crossfadeMode = \d|
		
		crossfadeMode = crossfadeMode.asString[0].toLower;
		
		// wfsp_d_2 : pre-panner for 2 arrays in dual mode (focused and normal separate panners )
		// wfsp_p_3 : pre-panner for 3 arrays plane wave
		
		^["wfsp", crossfadeMode, numArrays ].join("_");
	}
	
	*generateDef { |numArrays = 1, crossfadeMode = \dual|
		
		crossfadeMode = crossfadeMode.asString[0].toLower.asSymbol;
		
		^SynthDef( this.getDefName( numArrays, crossfadeMode ), {
			
			// synth args:
			var point = 0@0, pointFromBus = 0;
			var dbRollOff = -6, limit = 2, latencyComp = 0, pointLag = 0;
			var arrayConfs, cornerPoints, crossfadeLag = 0.2, pauseLag = 0.2;
			
			// local variables
			var input, panner, crossfader;
			var normalLevels, normalShouldRun, focusShouldRun;
			
			pointFromBus = \pointFromBus.kr( pointFromBus );
			point = (\point.kr( point.asArray ) * (1-pointFromBus)) + ( UIn.kr(0,2) * pointFromBus );
			pointLag = \pointLag.kr( pointLag );
			point = LPFLag.kr( point, pointLag );
			UOut.kr( 0, point );
			point = point.asPoint;
			
			if( crossfadeMode == \p ) { dbRollOff = 0 };
			
			dbRollOff = \dbRollOff.kr( dbRollOff );
			limit = \limit.kr( limit );
			
			// always static
			latencyComp = \latencyComp.ir( latencyComp );			
			// the pre-panner and delayed/attenuated output 
			panner = WFSPrePan( dbRollOff, limit, latencyComp );
			
			input = UIn.ar( 0, 1 );
			
			UOut.ar( 0, panner.ar( input, point, WFSLevelBus.kr ) * UEnv.kr( extraSilence: 0.2 ) );
			
			// crossfading: manage the array panners
			case { [ \d, \u ].includes(crossfadeMode) } { 
				
				// Point sources //
				
				#arrayConfs, cornerPoints = numArrays.collect({ |i| [ 
					("arrayConf" ++ i).asSymbol
						.ir( [ 48, 5, i.linlin(0,numArrays, 0.5pi, -0.5pi), 0, 0.164 ] ),
					("cornerPoints" ++ i).asSymbol.ir( [ 5, -5, 0.5pi, 0.5pi ] )
				] }).flop;
				
				
				crossfadeLag = \crossfadeLag.kr( crossfadeLag );
				pauseLag = \pauseLag.kr( pauseLag ); // extra time to wait before pause (not unpause)
				
				crossfader = WFSCrossfader( point, arrayConfs, cornerPoints );
							
				normalShouldRun = Slew.kr( crossfader.arraysShouldRun( false ), 
					1/crossfadeLag, 1/crossfadeLag );
				focusShouldRun = Slew.kr( crossfader.arraysShouldRun( true ), 
					1/crossfadeLag, 1/crossfadeLag );
				normalLevels = crossfader.cornerfades * normalShouldRun;
				 
				switch( crossfadeMode,
					\d, {  // dual mode
						
						{	// embed function to prevent inlining warning
							var normalIDs, focusIDs;
							var normalLevelBuses, focusLevelBuses, dontPause = 0;
							
							dontPause = \dontPause.kr(dontPause); // if 1 never pause
							
							// id's of synths to pause (998 for none) 
							normalIDs = \normalIDs.ir( 998.dup(numArrays) ).asCollection;
							focusIDs = \focusIDs.ir( 998.dup(numArrays) ).asCollection;
							
							// level buses (-1 for none)
							normalLevelBuses = \normalLevelBuses.kr( -1.dup(numArrays) ).asCollection;
							focusLevelBuses = \focusLevelBuses.kr( -1.dup(numArrays) ).asCollection;
							
							// output levels to appropriate buses (replace existing)
							normalLevelBuses.collect({ |bus, i|
								ReplaceOut.kr( bus, normalLevels[i] );
							});
							
							focusLevelBuses.collect({ |bus, i|
								ReplaceOut.kr( bus, focusShouldRun[i] );
							});
							
							// pause non-sounding panners
							normalIDs.collect({ |id, i|
								var pause;
								pause = (normalLevels[i] > 0);
								pause = Slew.kr( pause, inf, 1/pauseLag ) > 0;
								Pause.kr( pause.max(dontPause), id );
							});
							
							focusIDs.collect({ |id, i|
								var pause;
								pause = (focusShouldRun[i] > 0);
								pause = Slew.kr( pause, inf, 1/pauseLag ) > 0;
								Pause.kr( pause.max(dontPause), id );
							});
							
						}.value
					},
					\u, { // unified mode (focused and normal in one array panner)
						{
							var uniIDs;
							var uniLevelBuses;
							var uniLevels;
							var dontPause = 0;
							
							dontPause = \dontPause.kr(dontPause);
							
							// id's of synths to pause (-1 for none)
							uniIDs = \uniIDs.ir( -1.dup(numArrays) ).asCollection;
							
							// level buses (-1 for none)
							uniLevelBuses = \uniLevelBuses.kr( -1.dup(numArrays) ).asCollection;
							
							uniLevels = normalLevels.max( focusShouldRun );
							
							// output levels to appropriate buses (replace existing)
							uniLevelBuses.collect({ |bus, i|
								ReplaceOut.kr( bus, uniLevels[i] );
							});
							
							// pause non-sounding panners
							uniIDs.collect({ |id, i|
								var pause;
								pause = (uniLevels[i] > 0);
								pause = Slew.kr( pause, inf, 1/pauseLag ) > 0;
								Pause.kr( pause.max(dontPause), id );
							});
						}.value;					
					}
				);
				
			} { crossfadeMode === \p  } {
				
				// Plane wave //
				
				arrayConfs = numArrays.collect({ |i| 
					("arrayConf" ++ i).asSymbol
						.ir( [ 48, 5, i.linlin(0,numArrays, 0.5pi, -0.5pi), 0, 0.164 ] ) 
				});
				
				crossfader = WFSCrossfaderPlane( point, arrayConfs );
				
				{
					var planeIDs;
					var planeLevelBuses;
					var planeLevels;
					var dontPause = 0;
					
					dontPause = \dontPause.kr(dontPause);
					
					// id's of synths to pause (-1 for none)
					planeIDs = \planeIDs.ir( -1.dup(numArrays) ).asCollection;
					
					// level buses (-1 for none)
					planeLevelBuses = \planeLevelBuses.kr( -1.dup(numArrays) ).asCollection;
					
					planeLevels = crossfader.crossfades;
					
					// output levels to appropriate buses (replace existing)
					planeLevelBuses.collect({ |bus, i|
						ReplaceOut.kr( bus, planeLevels[i] );
					});
					
					// pause non-sounding panners
					planeIDs.collect({ |id, i|
						var pause;
						pause = (planeLevels[i] > 0);
						pause = Slew.kr( pause, inf, 1/pauseLag ) > 0;
						Pause.kr( pause.max(dontPause), id );
					});
				}.value;
			};
		});
		
	}
	
	*generateAll { |dir|
		dir = dir ? SynthDef.synthDefDir;
		synthDefs = crossfadeModes.collect({ |item|
			if( modesThatNeedArrays.includes( item ) ) {
				maxArrays.collect({ |i|
					this.generateDef( i+1, item ).writeDefFile( dir );
				});
			} {
				[ this.generateDef( 0, item ).writeDefFile( dir ) ]
			};
		});
		^synthDefs;		
	}
	
}

