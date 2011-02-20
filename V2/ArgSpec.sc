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
		default = this.constrain( default );
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

}

SynthArgSpec : ArgSpec {
	asSynthArg { |value|
		^[ name, this.constrain( value ) ]
	}
}

+ Array {
	asArgSpec { ^ArgSpec(*this) }
}

