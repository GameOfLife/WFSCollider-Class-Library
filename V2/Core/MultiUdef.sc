HiddenUdef : Udef {
	
	// these are not added to the global udef dict
	
	classvar <>all;
	
	*prefix { ^"uh_" } // synthdefs get another prefix to avoid overwriting
}


MultiUdef : Udef {

	var <>udefs;
	var <>chooseFunc;
	var >defNameKey;
	
	*defNameKey { ^\u_defName }
	defNameKey { ^defNameKey ? this.class.defNameKey }
	
	*new { |name, udefs, category, setter, setterIsPrivate = true| // first udef in list is default
		^super.basicNew( name, [ 
			ArgSpec( setter ? this.defNameKey, 
				udefs[0].name, ListSpec( udefs.collect(_.name) ), setterIsPrivate )
		], category )
			.udefs_( udefs );
	}
	
	findUdef{ |name|
		^udefs.detect({ |item| item.name == name }) ? udefs[0];
	}
	
	findUdefFor { |unit|
		^this.findUdef( unit.get( this.defNameKey ) );
	}
	
	asArgsArray { |argPairs, constrain = true|
		var defName, argz;
		defName = (argPairs ? []).detectIndex({ |item| item == this.defNameKey });
		if( defName.notNil ) {
			defName = argPairs[ defName + 1 ];
		} {
			defName = udefs[0].name;
		};
		argz = this.findUdef( defName ).asArgsArray( argPairs ? [], constrain );
		if( chooseFunc.notNil ) {
			defName = chooseFunc.value( argz );
		};
		^argz ++ [ this.defNameKey, defName ];
	}
	
	synthDef { ^udefs.collect(_.synthDef).flat }
	
	createSynth { |unit, target, startPos|
		^this.findUdefFor( unit ).createSynth( unit, target, startPos );
	}
		
	prIOids { |mode = \in, rate = \audio, unit|
		^this.findUdefFor( unit ).prIOids( mode, rate, unit );
	}
	
	canFreeSynth { |unit| ^this.findUdefFor( unit ).canFreeSynth( unit ) }
	
	chooseDef { |unit|
		var currentDefName, newDefName;
		if( chooseFunc.notNil ) {
			currentDefName = unit.get( this.defNameKey );
			newDefName = chooseFunc.value( unit.args );
			if( currentDefName != newDefName ) {
				unit.setArg( this.defNameKey, newDefName );
				unit.init( unit.def, unit.args );
			};
		};
	}
	
	setSynth { |unit ...keyValuePairs|
		this.chooseDef( unit ); // change def based on chooseFunc if needed
		if( keyValuePairs.includes( this.defNameKey ) ) {
			unit.init( unit.def, unit.args );
		} {
			^this.findUdefFor( unit ).setSynth( unit, *keyValuePairs );
		};
	}
	
	getSpec { |key, unit|
		if( key === this.defNameKey ) {
			^argSpecs[0].spec;
		} {
			^this.findUdefFor( unit ).getSpec( key, unit );
		};
	}
	
	argSpecs { |unit|
		^this.findUdefFor( unit ).argSpecs( unit ) ++ argSpecs;
	}
	
}