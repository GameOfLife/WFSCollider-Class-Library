WFSUnitDef {
	
	/*
	WFS lib 2.0
	W. Snoei 2010
	
	// use similar to Synth / SynthDef
	
	//example: 
	(
	d = WFSUnitDef( "sine", { |out = 0, freq = 440, amp = 0.1|
		Out.ar( out, SinOsc.ar( freq, 0, amp ) ) 
		}).load(s);
	)
		
	x = WFSUnit( "sine" );
		
	x.set( \freq, 770 );
	
	x.start(s);
	
	x.set( \freq, 330 );
	
	x.stop;
	
	x.get( \freq ); // -> returns current setting
	
	*/
	
	classvar <all;
	classvar <>defaultPrepareTime = 5;
	
	var <>name;		// Symbol
	var <>category;	// String / Symbol
	var <>type; 		// \source, \process ..
	var <>rate; 		// \audio, \control or [ \audio, \control ]
	var <>params; 	// ( paramName: [ spec, defaultValue (, func) ], ... )
	var <>bufferSpecs; // ( file: \soundFile,  ... ) // \soundFile
	var <>dict;		// Environment/Event for extra parameters
	var <>synthDefs; 	// [ SynthDef, ... ]
	var <>prepareTime;
	
	var <>prepareFunc;
	var <>startFunc; 	// Function
	var <>stopFunc;  	// Function
	var <>endFunc;
	
	var <>guiFuncs; 	// ( paramName: { arg value, wfsUnit;  })
	
	var <>cpuLoad = 1; // estimated cpu load
	
	*initClass {
		all = ();
	}
	
	*new { |name, ugenGraphFunc, autoCreateParams = true|
		name = name.asSymbol;
		^super.newCopyArgs( name ).init( ugenGraphFunc, autoCreateParams );
	}
	
	init { |ugenGraphFunc, autoCreateParams|
		if( ugenGraphFunc.isArray )
			{ ugenGraphFunc.do({ |func, i| // create multiple numbered
				this.createSynthDef( (name ++ i), func );
				});
			}
			{ this.createSynthDef( name, ugenGraphFunc ); };
			
		// parameters
		params = ();
		if( autoCreateParams ) {
			synthDefs[0].allControlNames.do({ |cn|
				params[ cn.name ] = [ cn.name, cn.defaultValue ];
			});
		};
		
		prepareFunc = this.defaultPreparefunc;
		startFunc = this.defaultStartfunc;
		stopFunc = this.defaultStopFunc;
		endFunc = this.defaultEndFunc;
				
		dict = (); // extra variables are stored here
		all[ name ] = this; // add to WFSUnitDef.all
	}
	
	createSynthDef { |inName, ugenGraphFunc| // create and add to array
		synthDefs = synthDefs.add( SynthDef( ("wfs_" ++ inName).asSymbol, ugenGraphFunc ) );
	}
	
	defaultPreparefunc { ^{ |unit, target, action| 
				// load buffers etc.
				action.value( unit, target );
				} }  
				
	defaultStartfunc { ^{ |unit, target| unit.createSynths( target ); } } // start synths
	defaultStopFunc { ^{ |unit| unit.endSynths; } } // stop synths
	defaultEndFunc { ^nil } // cleanup (after synths have ended)
	
	load { |server| server.asCollection.do({ |srv| synthDefs.do(_.load(srv)); }); }
	send { |server| server.asCollection.do({ |srv| synthDefs.do(_.send(srv)); }); }
	
	writeDefFile { |path| if( path.asSymbol.isArray.not ) { path = [ path ] };
		path.do({ |pth| synthDefs.do( _.writeDefFile( pth ) ) });
	}
	
}

WFSUnit {
	
	var <>def; 		// a WFSUnitDef (found via name)
	var <>settings; 	// ( paramName: value, ... )
	var <>synths; 	// [ Synth, ... ]
	var <>buffers;     // [ Buffer, ... ] (if any)
	var <gui; 		// ( paramName: [ view, ... ], ... )
	
	var <>prepared = false;
	
	*new { |name, args| // args: [ \paramName | WFSUnitDef, value, ... ]
		^super.new.init( name, args );
	}
	
	init { |name, args|
		def = name;
		if( def.class != WFSUnitDef )
			{ def = WFSUnitDef.all[ def.asSymbol ]; };
		if( def.isNil ) { "%:init WFSUnitDef( % ) not found".format( this.class, name ).error; };
		
		settings = ().putPairs( *args ); // a dict with the args
	}
	
	createSynth { |target, defIndex = 0, addAction=\addToTail|
		^target.asCollection.collect({ |trg|
			synths = synths.add( 
					Synth( def.synthDefs[defIndex].name, 
						settings.asKeyValuePairs, trg, addAction )
							.freeAction_({ |synth|									synths.remove( synth );
								this.doEndFunc( synth );
							})
						);
			});
	}
	
	set { |...pairs|
		pairs.pairsDo({ |key, value|
			settings[ key ] = value;
			this.changed( \set, key, value );
		});
		synths.do({ |synth| synth.set( *pairs ) });
	}
	
	get { |paramName|
		^settings[ paramName ] ?? { def.params[ paramName ][1] };
	}
	
	endSynths { synths.collect({ |synth| if( synth.isPlaying ) { synth.free}; }); synths = nil; }
	runSynths { |bool| synths.collect({ |synth| if( synth.isPlaying ) { synth.run(bool)}; }); }	
	prepare { |target, action| 
			def.prepareFunc.value( this, target, action );
			this.changed( \prepare ); 
		 }
	
	start { |target| def.startFunc.value( this, target ); this.changed( \start ); }
	stop { def.stopFunc.value( this ); this.changed( \stop ) }
	run { |bool = true| this.runSynths( bool ); this.changed(\run, bool) }
	
	doEndFunc { |synth| def.endFunc.value( this, synth ) }
	
}