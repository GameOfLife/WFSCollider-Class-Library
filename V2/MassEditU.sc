MassEditU : U { // mimicks a real U, but in fact edits multiple instances of the same
	
	var <units, <>argSpecs;
	
	*new { |units| // all units have to be of the same Udef
		^super.newCopyArgs.init( units );
	}
	
	init { |inUnits|
		var firstDef, defs;
		units = inUnits.asCollection;
		defs = inUnits.collect(_.def);
		firstDef = defs[0];
		if( defs.every({ |item| item == firstDef }) ) {
			def = firstDef;
			argSpecs = def.argSpecs.collect({ |argSpec|
				var values, massEditSpec;
				values = units.collect({ |unit|
					unit.get( argSpec.name );
				});
				massEditSpec = argSpec.spec.massEditSpec( values );
				if( massEditSpec.notNil ) {
					ArgSpec( argSpec.name, massEditSpec.default, massEditSpec, argSpec.private ); 
				} {
					nil;
				};
			}).select(_.notNil);
			args = argSpecs.collect({ |item| [ item.name, item.default ] }).flatten(1);
			
		} {
			"MassEditU:init - not all units are of the same Udef".warn;
		};
	}
	
	units_ { |inUnits| this.init( inUnits ); }
	
	resetArg { |key| // doesn't change the units
		if( key.notNil ) {
			this.setArg( key, def.getSpec( key ).massEditValue( 
				units.collect({ |unit| unit.get( key ) }) ) 
			);
		} {
			this.keys.do({ |key| this.resetArg( key ) });
		};
	}
	
	set { |...args|
		var synthArgs;
		args.pairsDo({ |key, value|
			var values;
			//value = value.asUnitArg( this );
			this.setArg( key, value );
			values = def.getSpec( key ).massEdit( units.collect(_.get(key) ), value );
			units.do({ |unit, i|
				unit.set( key, values[i] );
			});
		});
	}
	
	defName { ^((def !? { def.name }).asString + "(% units)".format( units.size )).asSymbol }
	
}