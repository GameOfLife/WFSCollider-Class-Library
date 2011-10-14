GenericDef {
	
	classvar <>all; // overwrite in subclass to create class specific lib
	
	var <>argSpecs;

	*new { |name, args|
		^super.new.initArgSpecs( args ).addToAll( name.asSymbol );
	}
	
	*fromName { |name|
		var def;
		this.all ?? { this.all = IdentityDictionary() };
		def = this.all[ name ];
		if( def.isNil ) {
            ^this.getFromFile(name);
		}{
		    ^def
		}
	}

	*getFromFile{ arg name;
		var path;
		^if( name.notNil and: {path = this.getDefFilePath(name); File.exists(path)} ) {
			path.load
		} {
			"//" + this.class ++ ": - no Udef found for % in %\n"
			.postf( this.cleanDefName(name), path );
			nil
		}
	}

	*loadAllFromDefaultDirectory {
	    ^(this.defsFolder++"/*.scd").pathMatch.collect({ |path| path.load })
	}

	*cleanDefName{ |name|
		^name.asString.collect { |char| if (char.isAlphaNum, char, $_) };
	}

	*getDefFilePath{ |defName|
		var cleanDefName = this.cleanDefName(defName);
		^this.defsFolder +/+ cleanDefName ++ ".scd";
	}

	initArgSpecs { |args|
		argSpecs = ArgSpec.fromArgsArray( args );
	}
	
	addToAll { |name|
		this.class.all ?? { this.class.all = IdentityDictionary() };
		this.class.all[ name ] = this;
	}
	
	name { ^this.class.all !? { this.class.all.findKeyForValue( this ); } }
	
	name_ { |name|
		this.class.all ?? { this.class.all = IdentityDictionary() };
		this.class.all[ this.name ] = nil;
		this.class.all[ name.asSymbol ] = this;
	}
	
	*allNames { ^this.class.all.keys.as( Array ).sort }
	
	addArgSpec { |argSpec, replaceIfExists = false|
		var index;
		if( argSpec.notNil ) {
			argSpec = argSpec.asArgSpec;
		};
		index = argSpecs.detectIndex({ |item| item.name == argSpec.name });
		if( index.isNil ) {
			argSpecs = argSpecs.add( argSpec );
		} {
			if( replaceIfExists ) {	 // otherwise leave in tact (use setArgSpec to change spec)
				argSpecs[index] = argSpec;
			};
		};
	}
	
	removeArgSpec { |name|
		var index;
		if( name.isKindOf( ArgSpec ) ) { name = name.name };
		index = this.getArgIndex( name );
		if( index.notNil ) {
			^argSpecs.removeAt( index );
		} {
			^nil
		};
	}
	
	
	// getters 
	
	getArgIndex { |name|
		name = name.asSymbol;
		^argSpecs.detectIndex({ |item| item.name == name });
	}
	
	getArgSpec { |name|
		name = name.asSymbol;
		^argSpecs.detect({ |item| item.name == name });
	}
	
	getSpec { |name|
		var asp;
		asp = this.getArgSpec(name);
		if( asp.notNil ) { ^asp.spec } { ^nil };
	}
	
	getDefault { |name|
		var asp;
		asp = this.getArgSpec(name);
		if( asp.notNil ) { ^asp.default } { ^nil };
	}
	
	// setters
	
	setArgSpec { |argSpec|
		var index;
		argSpec = argSpec.asArgSpec;
		index = this.getArgIndex( argSpec.name );
		if( index.notNil ) { 
			argSpecs[index] = argSpec;
		} { 
			"%:setArgSpec - can't set because arg % for % not found"
				.format( this.class, argSpec.name, this.name )
				.warn;
		};
	}
	
	setDefault { |name, default|
		var asp;
		asp = this.getArgSpec(name);
		if( asp.notNil ) { asp.default = default ? asp.default };
	}
	
	setSpec { |name, spec|
		var asp;
		asp = this.getArgSpec(name);
		if( asp.notNil ) { asp.spec = spec.asSpec };
	}

	/**
	* argPairs -> an array with key/value pairs
	* returns -> an array with key/value pairs for all arguments of the synthdef.
	* if the argPairs arrays doesn't have a certain argument, the default value
	* is used.
	*/
	asArgsArray { |argPairs, constrain = true|
		argPairs = argPairs ? #[];
		^argSpecs.collect({ |item| 
			var val;
			val = argPairs.pairsAt(item.name) ?? { item.default.copy };
			if( constrain ) { val = item.constrain( val ) };
			[ item.name,  val ] 
		}).flatten(1);
	}
	
	args { ^argSpecs.collect({ |item| [ item.name, item.default ] }).flatten(1) } 
	
	constrain { |...nameValuePairs|
		^nameValuePairs.clump(2).collect({ |name, value|
			this.prConstrain(name, value);
		}).flatten(1);
	}
	
	prConstrain { |name, value|
		^[ name, this.getArgSpec( name ).constrain( value ) ];
	}
	
	keys { ^argSpecs.collect(_.name) }
	argNames { ^argSpecs.collect(_.name) }
		
	values { ^argSpecs.collect(_.default) }
	
	specs { ^argSpecs.collect(_.spec) }
	
    archiveAsCompileString { ^true }

}

