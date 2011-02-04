// wslib 2010

// creates a function of which the args can be changed and kept
// functions are stored in a named bank of defs: TransformDef.all

/*

TransformDef( \add, { |in, add = 0, mul = 1| (in*mul) + add }); // create and store a def

x = Transform( \add ); // create a transform

x.value( 10 ); // -> 10 (func is bypassed when no arguments are changed)

x.add = 5; // change argument

x.value( 10 ); // -> 15

x.value( 10, 2 ); // -> 12 (change argument on the fly)

// it also sends a 'changed' message at each argument change

x.doOnChange( \add, { |tr, key, value| [ key, value].postln }, false );

x.add = 7; // -> [ add, 7 ]

x.def.specs[ \add ] = [-10,10]; // define a controlspec for add parameter
x.def.specs[ \mul ] = [0,2];

y = x.gui; // a TransformGUI

y.action = { x.value( 10 ).postln };

x.add = 5; // watch the slider move

*/

ObjectWithArgs {
	
	var <args;
	
	// args are an array of key, value pairs: [ key, value, key, value ...etc ]
	
	keys { ^(args ? [])[0,2..] }
	argNames { ^this.keys }
	
	values { ^(args ? [])[1,3..] }
	values_ { |newValues|  
		var keys = this.keys;
		newValues[..keys.size-1].do({ |val, i|
			this.setArg( keys[i], val )
		});
	}

	setArg { |key, value|
		var index;
		index = this.keys.indexOf( key );
		if( index.notNil ) { 
			args[ (index * 2) + 1 ] = value;
			this.changed( key, value );
		} {
			"%:% arg % not found".format( this.class, thisMethod.name, key ).warn;
		};	
	}
	
	getArg { |key|
		var index;
		index = this.keys.indexOf( key );
		if( index.notNil ) { 
			^args[ (index * 2) + 1 ] 
		} { 
			"%:% arg % not found".format( this.class, thisMethod.name, key ).warn;
			^nil 
		};
	}
	
	at { |key| ^this.getArg( key ) }
	put { |key| ^this.setArg( key ) }

	doesNotUnderstand { |selector ...args| 
		// bypasses errors; warning if arg not found
		if( selector.isSetter ) { 
			this.setArg( selector.asGetter, *args ) 
		} {
			^this.getArg( selector );
		};	
	}

}

TransformDef : ObjectWithArgs {
	
	classvar <>all;
	
	var <>func, <>category;
	var <>bypassFunc, <startIndex;
	var <>specs;
	
	*initClass {
		this.all = IdentityDictionary();
	}
	
	*new { |name, func, args, category = \default|
		^super.new.init( func, args ).addToAll( name.asSymbol );
	}
	
	*defaultFunc { ^{ |in, mul = 1, add = 0| (in * mul) + add } }
	
	*defaultStartIndex { ^1 }
	
	startIndex_ { |newIndex = 1| startIndex = newIndex; this.init; }
	
	init { |inFunc, inArgs|
		var argNames, values;
		
		func = inFunc ?? { this.defaultFunc };
		
		startIndex = startIndex ? this.class.defaultStartIndex;
		
		specs = IdentityDictionary();
		
		argNames = (func.argNames ? #[])[startIndex..]; // first arg is input
		values = func.def.prototypeFrame ? #[];
		inArgs = inArgs ? args ? #[]; // keep old args at re-init
		
		args = argNames.collect({ |key, i|
			[ key, inArgs.pairsAt( key ) ?? { values[i+startIndex] } ]
		}).flatten(1);
		
		bypassFunc = { |tr| tr.values == tr.def.values }; // if default values
	}
	
	addToAll { |name|
		this.class.all[ name ] = this;
	}
	
	name { ^all.findKeyForValue( this ); }
	
	name_ { |name|
		this.class.all[ this.name ] = nil;
		this.class.all[ name.asSymbol ] = this;
	}
	
	*allNames { ^this.class.all.keys.as( Array ).sort }
}


Transform : ObjectWithArgs {
	
	var <def, <>makeCopy = false;
	
	*new { |defName, args|
		^super.new.init( defName, args ? [] );
	}
	
	*defClass { ^TransformDef }
	
	init { |inName, inArgs|
		def = this.class.defClass.all[ inName.asSymbol ];
		if( def.notNil ) {	
			args = def.args.deepCopy;
			inArgs.keysValuesDo({ |key, value|
				args.pairsPut( key, value );
			});
		} { 
			"defName '%' not found".format(inName).warn; 
		};
	}
	
	value { |...inArgs| // in place by default
		this.values = inArgs[ def.startIndex..];
		if( def.bypassFunc.value( this ).not ) { 
			if( makeCopy) { inArgs[0] = inArgs[0].copyNew };
			^this.prValue( *inArgs[..def.startIndex - 1] ); 
		} {
			^(inArgs ? #[])[0] 
		};
	}
	
	prValue { |...inArgs|
		^def.func.value( 
			*(( inArgs.extend( def.startIndex, nil ) ++ this.values )
				.collect(_.value)) // support functions and streams
		);
	}	
	
	defName_ { |name, keepArgs = true|
	  	this.init( name.asSymbol, if( keepArgs ) { args } { [] }); // keep args
	}
	
	reset { this.values = this.def.values.deepCopy; }
	
	defName { ^def.name }
}


+ SequenceableCollection {
	
	pairsAt { |key| // could be optimized
		var index = this[0,2..].indexOf( key );
		if( index.notNil ) { 
			^this.at( (index * 2) + 1 );
		} {
			^nil 
		};
	}
	
	pairsPut { |key, value| // only puts if key is found
		var index = this[0,2..].indexOf( key );
		if( index.notNil ) { 
			this.put((index * 2) + 1, value);
		};
	}
}
