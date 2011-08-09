/*
f = { |a=1, b=2| a+b };

x = ArgSpec.fromFunc( f ); // array of ArgSpecs

x.postln; // -> [ an ArgSpec(a, 1), an ArgSpec(b, 2) ]

*/


ArgSpec : Spec {
	var <>name, <>default, <>spec;
	var <>private = false;
	var >label;
	
	*new { |name, default, spec, private|
		^super.newCopyArgs( name, default, spec, private ? false ).init;
	}
	
	init { 
		spec = (spec ? name).asSpec; 
		if( spec.isNil ) {
			spec = Spec.forObject( default );
		};
		if( spec.notNil ) { default = default ? spec.default };
		//default = this.constrain( default );
	}
	
	doWithSpec { |selector, value ...more| 
		^spec.tryPerform( selector, value, *more ) ? value; 
	}
	
	label { ^label ? name }
		
	constrain { |value|
		^this.doWithSpec( \constrain, value );
	}
	
	asSynthArg { |value|
		^[]
	}
	
	map { |value|
		^this.doWithSpec( \map, value );
	}
	
	unmap { |value|
		^this.doWithSpec( \unmap, value );
	}
	
	makeView { |parent, bounds, label, action, resize|
		var vws;
		if( private.not ) { // no view if private
			case { label.isNil } { label = this.label }
				{ label == false } { label = nil }; 
			vws = spec.asSpec.makeView( parent, bounds, label, action, resize );
			if( default.notNil ) { this.setView( vws, default, false ) };
			^vws;
		} { 
			^nil;
		};
	}	
	
	setView { |view, value, active = false|
		spec.asSpec.setView( view, value, active );
	}
	
	mapSetView { |view, value, active = false|
		spec.asSpec.mapSetView( view, value, active );
	}
	
	asArgSpec { ^this }
	
	printOn { arg stream;
		stream << "an " << this.class.name << "(" <<* [name, default]  <<")"
	}

	storeOn { arg stream;
		stream << this.class.name << "(" <<* [
			name.asCompileString, 
			default.asCompileString, 
			( spec.findKey ? spec ).asCompileString, 
			private
		]  <<")"
	}

	*fromArgsArray { |args| // creates array of ArgSpecs
		
		if( args.notNil && { args[0].class == Symbol } ) { // assume synth-like arg pairs
			args = args.clump(2);
		};
		
		^args.collect({ |item| item.asArgSpec });
	}
	
	*fromFunc { |func, args| // creates array of ArgSpecs
		var argNames, values, inArgNames;
		
		args = this.fromArgsArray( args ); // these overwrite the ones found in the func
		inArgNames = args.collect(_.name);
		
		argNames = (func.argNames ? #[]);
		values = func.def.prototypeFrame ? #[];
		
		^argNames.collect({ |key, i|
			var inArgIndex;
			inArgIndex = (inArgNames ? []).indexOf( key );
			if( inArgIndex.isNil ) {
				ArgSpec( key, values[i] );
			} {
				args[ inArgIndex ];
			};
		});
		
	}
	
	*fromSynthDef { |synthDef, args| // creates array of ArgSpecs
		var argNames, values, inArgNames;
		var allControlNames;
		
		args = this.fromArgsArray( args ); // these overwrite the ones found in the func
		inArgNames = args.collect(_.name);
		
		allControlNames = synthDef.allControlNames;
		argNames = (allControlNames.collect(_.name) ? #[]);
		values = allControlNames.collect(_.defaultValue) ? #[];
		
		^argNames.collect({ |key, i|
			var inArgIndex;
			inArgIndex = (inArgNames ? []).indexOf( key );
			if( inArgIndex.isNil ) {
				ArgSpec( key, values[i] );
			} {
				args[ inArgIndex ];
			};
		});
		
	}


}

SynthArgSpec : ArgSpec {
	asSynthArg { |value|
		^[ name, this.constrain( value ) ]
	}
}

+ Array {
	asArgSpec { ^ArgSpec(*this) }
}

