SimpleEditFunc : ObjectWithArgs {
	
	// function format:
	// { |this, objectToEdit|  ... }
	
	// defaults can be function:
	// { |this, objectToGetDefaultsFrom| ... }
	// should output an array in the same form of args:
	// [ \argName, value, \argName, value ...etc ]

	var <>name;
	var <>func;	
	var >defaults;
	var <>specs;
	var <>action;
	var <>makeView;
	var <>bypassFunc;
	var <>makeCopy = false;
	
	*new { |name, func, args, defaults|
		^super.new.init( name, func, args, defaults );
	}
	
	init { |inName, inFunc, inArgs, inDefaults|
		var keys, values;
		
		defaults = inDefaults ?? { inArgs.deepCopy };
		args = inArgs;
		func = inFunc;
		specs = args.clump(2).collect({ |item|
			var spec;
			spec = (spec ? item[0]).asSpec; 
			if( spec.isNil ) {
				spec = Spec.forObject( item[1] );
			};
			spec;
		});
		bypassFunc = { |me, obj| me.defaults( obj ) == me.args };
	}
	
	setSpec { |argName, spec|
		var index;
		index = this.keys.indexOf( argName );
		if( index.notNil ) {
			specs[ index ] = spec;
		} {
			"%:setSpec - argName % not found".format( this.class, argName ).warn;
		};
	}
	
	getSpec { |argName|
		var index;
		index = this.keys.indexOf( argName );
		if( index.notNil ) {
			^specs[ index ];
		} {
			"%:setSpec - argName % not found".format( this.class, argName ).warn;
			^nil;
		};
	}
	
	reset { |obj| // can use an object to get the defaults from
		this.args = defaults.value( this, obj );
	}
	
	args_ { |newArgs, constrain = false|
		newArgs.pairsDo({ |argName, value|
			this.set( argName, value, constrain );
		});
	}
	
	defaults { |obj|
		^defaults.value( this, obj );
	}
	
	set { |argName, value, constrain = false|
		var spec;
		if( constrain && { (spec = this.getSpec( argName )).notNil } ) { 
			value = spec.constrain( value );
		};
		this.setArg( argName, value );
		this.changed( argName, value );
	}
	
	get { |argName| ^this.getArg( argName ) }
	
	doesNotUnderstand { |selector ...args| 
		// bypasses errors; warning if arg not found
		if( selector.isSetter ) { 
			this.set( selector.asGetter, *args ) 
		} {
			^this.get( selector );
		};	
	}
	
	checkBypass { |obj| ^this.bypassFunc.value( this, obj ) != true }
	
	value { |obj, args|
		this.args = args;
		if( makeCopy ) { obj = obj.deepCopy };
		if( this.checkBypass( obj ) ) {
			^this.prValue( obj );
		} {
			^obj;
		};

	}
	
	prValue { |obj|
		^func.value( this, obj );
	}
	
}