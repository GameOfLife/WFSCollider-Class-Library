WFSUnitDef : GenericDef {
	
	classvar <>all;
	
	var <>func, <>category;
	var <>synthDef;
	
	*initClass {
		this.all = IdentityDictionary();
	}
	
	*new { |name, func, args, category|
		^super.new( name, args ).init( func, name ).category_( category ? \default );
	}
	
	*prefix { ^"wfsu_" }
		
	init { |inFunc, name|
		var argNames, values;
		
		func = inFunc;
		
		synthDef = SynthDef( this.class.prefix ++ name.asString, func );
		
		argSpecs = ArgSpec.fromFunc( func, argSpecs ); // leave out the first arg 
	}
	
	addToAll { |name|
		this.class.all[ name ] = this;
	}
	
	loadSynthDef { |server|
		synthDef.load(server);
	}
	
	sendSynthDef { |server|
		synthDef.send(server);
	}
	
	synthDefName { ^synthDef.name }
	
	load { |server| this.loadSynthDef( server ) }
	send { |server| this.sendSynthDef( server ) }
	
	*allNames { ^this.class.all.keys.as( Array ).sort }
	
}

WFSUnit : ObjectWithArgs {
	
	var <def;
	var <>synths;
	
	*new { |defName, args|
		^super.new.init( defName, args ? [] );
	}
	
	*defClass { ^WFSUnitDef }
	
	init { |inName, inArgs|
		def = this.class.defClass.fromName( inName.asSymbol );
		if( def.notNil ) {	
			args = def.asArgsArray( inArgs );
		} { 
			"defName '%' not found".format(inName).warn; 
		};
		synths = [];
	}	
	
	set { |key, value|
		this.setArg( key, value );
		synths.do(_.set(key, value));
	}
	
	get { |key|
		^this.getArg( key );
	}
	
	doesNotUnderstand { |selector ...args| 
		// bypasses errors; warning only if arg not found
		if( selector.isSetter ) { 
			this.set( selector.asGetter, *args ) 
		} {
			^this.get( selector );
		};	
	}
	
	defName_ { |name, keepArgs = true|
	  	this.init( name.asSymbol, if( keepArgs ) { args } { [] }); // keep args
	}
	
	start { |server| 
		var synth;
		synth = Synth( def.synthDefName, args, server )
				.startAction_({ |synth|
					synths = synths ++ [ synth ]; 
						// only add if started (in case this is a bundle)
					this.changed( \go, synth ); 
				})
				.freeAction_({ |synth| 
					synths.remove( synth ); 
					this.changed( \end, synth ); 
				});
		this.changed( \start, synth );
		^synth;
		}
	
	free { synths.do(_.free) } 
	stop { this.free }
	
	resetSynths { synths = []; } // after unexpected server quit
	resetArgs { this.values = this.def.values.deepCopy; synths.do(_.set( *args )) }
	
	defName { ^def.name }
	
}