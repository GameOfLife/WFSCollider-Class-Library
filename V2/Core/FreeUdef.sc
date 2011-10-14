FreeUdef : Udef {
	
	// a freeUDef can hold any function for any type of functionality
	// fully customizable
	
	// please note that the createSynthFunc must return a single synth
	// if there are more synths started please either add them to the env
	// or to the unit.synths in the func. This synth will be tracked to
	// let the unit know if it stopped or not. It may also be a Group,
	// but in that case any UEnv will not work (unless applied to a synth
	// outside the group).

	var <>createSynthFunc, <>setSynthFunc;
	var <>env; // environment for variables
	
	var <>createSynthDefFunc; // optional, called at load or send
	
	*new { |name, args, canFreeSynth = false, category|
		^super.basicNew( name, args ? [], category ).initFree( canFreeSynth ); 
	}
	
	initFree { | canFreeSynth |
		env = ();
		if( canFreeSynth ) { this.addUEnv };
		this.initArgs;
	}
	
	initArgs {
		argSpecs.do({ |item|
			if( item.name.asString[..1].asSymbol == 'u_' ) { item.private = true; };
		});
	}
	
	addSynthDefControls { |def|
		def = def ? synthDef;
		ArgSpec.fromSynthDef( def ).do({ |argSpec| this.addArgSpec( argSpec ); });
		this.initArgs; // make private if needed
	}
	
	removeSynthDefControls { |def|
		def = def ? synthDef;
		ArgSpec.fromSynthDef( def ).do({ |argSpec| this.removeArgSpec( argSpec ); });
	}
	
	addUIO { |class, selector ...args| 
		// create a temp synthdef to get the correct args 
		// this assumes you have a UEnv in at least one of 
		// the synths you are running, or you use the UEnv controls in 
		// some other way to release the synths, set its duration etc
		this.addSynthDefControls( SynthDef( "tmp", { class.perform( selector, *args) } ) ); 
	}
	
	removeUIO { |class, selector ...args|
		this.removeSynthDefControls( SynthDef( "tmp", { class.perform( selector, *args) } ) ); 	}
	
	addUEnv { this.addUIO( UEnv, \kr ); }
	removeUEnv {  this.removeUIO( UEnv, \kr ); }
		
	envPut { |key, value|
		env.put( key, value );
	}
	
	canFreeSynth_ { |bool| 
		if( bool ) { this.addUEnv } { this.removeEnv };
	}

	
	createSynthDef { synthDef = createSynthDefFunc.value( this ) ? synthDef; }
		
	loadSynthDef { |server|
		this.createSynthDef;
		if( synthDef.notNil ) {
			server = server ? Server.default;
			server.asCollection.do{ |s|
		   		synthDef.asCollection.do(_.send(s))
			}
		}
	}
	
	sendSynthDef { |server|
		this.createSynthDef;
		if( synthDef.notNil ) {	
			server = server ? Server.default;
			server.asCollection.do{ |s|
				synthDef.asCollection.do(_.send(s));
			}
		}
	}
	
	synthDefName { 
		if( synthDef.notNil ) { 
			if( synthDef.isArray ) {
				^synthDef.collect(_.name) 
			} {
				^synthDef.name;
			};
		} { 
			^nil 
		} 
	}
	
	createSynth { |unit, server, startPos = 0| // create a single synth based on server
		if( createSynthFunc.notNil ) {
			server = server ? Server.default;
			^createSynthFunc.value( unit, server, startPos );
		} {
			^super.createSynth( unit, server, startPos );
		};
	} 
	
	setSynth { |unit ...keyValuePairs|
		if( setSynthFunc.notNil ) {
			setSynthFunc.value( unit, *keyValuePairs );
		} { 
			super.setSynth( unit, *keyValuePairs );
		};
	}
	
	printOn { arg stream;
		stream << "a " << this.class.name << "(" <<* [this.name]  <<")"
	}

	storeOn { arg stream;
		stream << this.class.name << "(" <<* [
			this.name.asCompileString, 
			argSpecs.asCompileString,
			createSynthFunc.asCompileString,
			setSynthFunc.asCompileString,
			this.canFreeSynth,
			category.asCompileString
		]  <<")"
	}

}