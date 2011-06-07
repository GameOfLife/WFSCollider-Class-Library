/*

// example

(
x = WFSUnitDef( \sine, { |freq = 440, amp = 0.1| 
	Out.ar( 0, SinOsc.ar( freq, 0, amp ) ) 
} );
)

y = WFSUnit( \sine, [ \freq, 880 ] );

y.gui;

y.def.loadSynthDef;

y.start;
y.stop;

(
// a styled gui in user-defined window
w = Window( y.defName, Rect( 300,25,200,200 ) ).front;
w.addFlowLayout;
RoundView.useWithSkin( ( 
	labelWidth: 40, 
	font: Font( Font.defaultSansFace, 10 ), 
	hiliteColor: Color.gray(0.25)
), { 
	SmoothButton( w, 16@16 )
		.label_( ['power', 'power'] )
		.hiliteColor_( Color.green.alpha_(0.5) )
		.action_( [ { y.start }, { y.stop } ] )
		.value_( (y.synths.size > 0).binaryValue );
	y.gui( w );
});
)

*/

WFSUnitDef : GenericDef {
	
	classvar <>all;
	
	var <>func, <>category;
	var <>synthDef;
	
	*new { |name, func, args, category|
		^super.new( name, args ).init( func ).category_( category ? \default );
	}
	
	*prefix { ^"wfsu_" }
		
	init { |inFunc|
		var argNames, values;
		
		func = inFunc;
		
		synthDef = SynthDef( this.class.prefix ++ this.name.asString, func );
		
		argSpecs = ArgSpec.fromSynthDef( synthDef, argSpecs );
		
		argSpecs.do({ |item|
			if( item.name.asString[..4].asSymbol == 'wfsu_' ) {
				item.private = true;
			};
		});
	}
	
	// this may change
	loadSynthDef { |server|
		synthDef.load(server);
	}
	
	sendSynthDef { |server|
		synthDef.send(server);
	}
	
	synthDefName { ^synthDef.name }
	
	load { |server| this.loadSynthDef( server ) }
	send { |server| this.sendSynthDef( server ) }
	
	// these may differ in subclasses of WFSUnitDef
	createSynth { |unit, server|
		if( server.isNil ) { server = Server.default; };
		^server.asCollection.collect({ |server|
			Synth( this.synthDefName, unit.args, server, \addToTail )
		});
	}
	
	setSynth { |unit ...keyValuePairs|
		unit.synths.do(_.set(*keyValuePairs));
	}
	
	printOn { arg stream;
		stream << "a " << this.class.name << "(" <<* [this.name]  <<")"
	}

	storeOn { arg stream;
		stream << this.class.name << "(" <<* [
			this.name.asCompileString, 
			func.asCompileString,
			argSpecs.asCompileString,
			category.asCompileString
		]  <<")"
	}
		
}

WFSUnit : ObjectWithArgs {
	
	var <def;
	var <>synths;
	
	*new { |defName, args|
		^super.new.init( defName, args ? [] );
	}
	
	*defClass { ^WFSUnitDef }
	
	init { |inName, inArgs|
		if( inName.isKindOf( this.class.defClass ) ) {
			def = inName;
		} {
			def = this.class.defClass.fromName( inName.asSymbol );
		};
		if( def.notNil ) {	
			args = def.asArgsArray( inArgs );
		} { 
			"defName '%' not found".format(inName).warn; 
		};
		synths = [];
	}	
	
	set { |key, value|
		this.setArg( key, value );
		def.setSynth( this, key, value );
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
	
	start { |server| // call this from inside Server:makeBundle
		var synth;
		^def.createSynth( this, server ).asCollection.do({ |synth|
			synth.startAction_({ |synth|
				synths = synths ++ [ synth ];
				this.changed( \go, synth ); 
			});
			synth.freeAction_({ |synth| 
				synths.remove( synth ); 
				this.changed( \end, synth ); 
			});
			this.changed( \start, synth );
		});
	}
	
	free { synths.do(_.free) } 
	stop { this.free }
	
	resetSynths { synths = []; } // after unexpected server quit
	resetArgs {
		this.values = this.def.values.deepCopy; 
		def.setSynth( this, *args );
	}
	
	defName { ^def !? { def.name } }
	
	printOn { arg stream;
		stream << "a " << this.class.name << "(" <<* [this.defName, args]  <<")"
	}

	storeOn { arg stream;
		stream << this.class.name << "(" <<* [
			( this.defName ? this.def ).asCompileString,
			args.asCompileString
		]  <<")"
	}
	
	asWFSUnit { ^this }
	
}


+ Symbol { 
	asWFSUnit { |args| ^WFSUnit( this, args ) }
}
