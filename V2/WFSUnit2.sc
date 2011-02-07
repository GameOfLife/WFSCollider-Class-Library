WFSUnitDef2 : ObjectWithArgs {
	
	classvar <>all;
	
	var <>func, <>category;
	var <>synthDef;
	var <>specs;
	
	*initClass {
		this.all = IdentityDictionary();
	}
	
	*new { |name, func, args, category = \default|
		^super.new.init( func, args, category, name ).addToAll( name.asSymbol );
	}
	
	*fromName { |name|
		^all[ name.asSymbol ];
	}
	
	*prefix { ^"wfsu_" }
		
	init { |inFunc, inArgs, inCategory, name|
		var argNames, values;
		
		func = inFunc;
		category = inCategory;
		
		synthDef = SynthDef( this.class.prefix ++ name.asString, func );
		
		specs = IdentityDictionary();
		
		argNames = func.argNames ? #[]; // first arg is input
		values = func.def.prototypeFrame ? #[];
		inArgs = inArgs ? args ? #[]; // keep old args at re-init
		
		args = argNames.collect({ |key, i|
			[ key, inArgs.pairsAt( key ) ?? { values[i] } ]
		}).flatten(1);
		
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
	
	name { ^all.findKeyForValue( this ); }
	
	name_ { |name|
		this.class.all[ this.name ] = nil;
		this.class.all[ name.asSymbol ] = this;
	}
	
	*allNames { ^this.class.all.keys.as( Array ).sort }
	
}

WFSUnit2 : ObjectWithArgs {
	
	var <def;
	var <>synths;
	
	*new { |defName, args|
		^super.new.init( defName, args ? [] );
	}
	
	*defClass { ^WFSUnitDef2 }
	
	init { |inName, inArgs|
		def = this.class.defClass.fromName( inName.asSymbol );
		if( def.notNil ) {	
			args = def.args.deepCopy;
			inArgs.keysValuesDo({ |key, value|
				args.pairsPut( key, value );
			});
		} { 
			"defName '%' not found".format(inName).warn; 
		};
		synths = [];
	}	
	
	set { |key, value|
		this.setArg( key, value );
		synths.do(_.set(key, value));
	}
	
	get { |key, value|
		this.getArg( key, value );
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
					synths = synths ++ [ synth ]; // only add if started (in case this is a bundle)
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
	stop { synths.do(_.free) }
	
	resetSynths { synths = []; } // after unexpected server quit
	resetArgs { this.values = this.def.values.deepCopy; synths.do(_.set( *args )) }
	
	defName { ^def.name }
	
}