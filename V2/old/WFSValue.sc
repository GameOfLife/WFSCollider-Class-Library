WFSValue {Ê
	
	var >value = 0;
	var <>modulators;

	*new {Ê|value ...modulators|
		^super.newCopyArgs( value, modulators );
	}
		
	addModulator { |key|
		modulators = modulators ++ [ key ]; // check if already there?
	}
	
	value {   
		var outValue;
		outValue = value;
		modulators.do({ |key|
			var mod;
			mod = WFSModulator.fromKey(key);
			mod !? { outValue = mod.calculate( outValue ); };
		});
		^outValue;
	}
	
	normalize { value = this.value; modulators = []; } // create new value and remove modulators
	
}



WFSModulator {
	classvar <all;

	var <>value;
	var <>func;
	
	*initClass {
		all = IdentityDictionary();
	}
	
	*new { |key = \level, value = 1, func|
		^super.newCopyArgs( value, func ).init.addToAll( key );
	}
	
	*fromKey { |key|
		^all[ key ];
	}
	
	key { ^all.findKeyForValue }
	
	init { func = func ?? { { |inVal, modVal| inVal * modVal } }; }
	
	addToAll { |key| all[ key ] = this;  }
	
	calculate { |inVal| ^func.value( inVal, value ); }
}