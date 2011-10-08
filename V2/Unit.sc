/*


Udef -> *new { |name, func, args, category|
    name: name of the Udef and corresponding unit
    func: ugen graph function
    args:  array with argName/default pairs
    category: ?

     -> defines a synthdef, and specs for the argumetns of the synthdef
     -> Associates the unitDef with a name in a dictionary.

U -> *new { |defName, args|
	Makes new Unit based on the defName.
	Retrieves the corresponding Udef from a dictionary
	sets the current args


// example

//using builtin Udefs
//looks for the file in the Udefs folder
x  = U(\sine)
x.def.loadSynthDef
x.start
(
x = Udef( \sine, { |freq = 440, amp = 0.1|
	Out.ar( 0, SinOsc.ar( freq, 0, amp ) ) 
} );
)

y = U( \sine, [ \freq, 880 ] );
y.gui;

y.def.loadSynthDef;

y.start;
y.stop;

y.set( \freq, 700 );

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

Udef : GenericDef {
	
	classvar <>all, <>defsFolder;
	
	var <>func, <>category;
	var <>synthDef;
	var <>shouldPlayOnFunc;
	var <>apxCPU = 1; // indicator for the amount of cpu this unit uses (for load balancing)

	*initClass{
		defsFolder = this.filenameSymbol.asString.dirname.dirname +/+ "UnitDefs";
	}

	*basicNew { |name, args, category|
		^super.new( name, args ).category_( category ? \default );
	}
	
	*new { |name, func, args, category|
		^super.new( name, args ).init( func ).category_( category ? \default );
	}
	
	*prefix { ^"u_" }
		
	init { |inFunc|
		var argNames, values;
		
		func = inFunc;
		
		synthDef = SynthDef( this.class.prefix ++ this.name.asString, func );
		
		argSpecs = ArgSpec.fromSynthDef( synthDef, argSpecs );
		
		argSpecs.do({ |item|
			if( item.name.asString[..1].asSymbol == 'u_' ) {
				item.private = true;
			};
		});
		this.changed( \init );
	}
	
	// this may change 
	// temp override to send instead of load (remote servers can't load!!)
	loadSynthDef { |server|
		server = server ? Server.default;
		server.asCollection.do{ |s|
		    synthDef.send(s)
		}
	}
	
	sendSynthDef { |server|
		server = server ? Server.default;
		server.asCollection.do{ |s|
			synthDef.send(s)
		}
	}
	
	synthDefName { ^synthDef.name }
	
	load { |server| this.loadSynthDef( server ) }
	send { |server| this.sendSynthDef( server ) }
	
	makeSynth { |unit, target, startPos = 0, synthAction|
	    var synth;
	    if( unit.shouldPlayOn( target ) != false ) {
		    /* // maybe we don't need this, or only at verbose level
		    if( unit.preparedServers.includes( target.asTarget.server ).not ) {
				"U:makeSynth - server % may not (yet) be prepared for unit %"
					.format( target.asTarget.server, this.name )
					.warn;
			};
			*/
			synth = this.createSynth( unit, target, startPos );
			synth.startAction2_({ |synth|
				unit.changed( \go, synth );
			});
			synth.freeAction2_({ |synth|
				unit.removeSynth( synth );
				synth.server.loadBalancerAddLoad( this.apxCPU.neg );
				unit.changed( \end, synth );
				if(unit.disposeOnFree) {
					unit.disposeArgsFor(synth.server)
				}
			});
			unit.changed( \start, synth );
			synthAction.value( synth );
			unit.addSynth(synth);
		};
	}
	
	shouldPlayOn { |unit, server| // returns nil if no func
		^shouldPlayOnFunc !? { shouldPlayOnFunc.value( unit, server ); }
	}
	
	// I/O
	
	prGetIOKey { |mode = \in, rate = \audio ... extra|
		^([ 
			"u", 
			switch( mode, \in, "i", \out, "o" ),  
			switch( rate, \audio, "ar", \control, "kr" )
		] ++ extra).join( "_" );
	}
	
	prIOspecs { |mode = \in, rate = \audio, key|
		key = key ?? { this.prGetIOKey( mode, rate ); };
		^argSpecs.select({ |item|
			var name;
			name = item.name.asString;
			name[..key.size-1] == key &&
			 	{ name[ name.size - 3 .. ] == "bus" };
		});
	}
	
	prIOids { |mode = \in, rate = \audio|
		var key;
		key = this.prGetIOKey( mode, rate );
		^this.prIOspecs( mode, rate, key ).collect({ |item|
			item.name.asString[key.size+1..].split( $_ )[0].interpret;
		});
	}
	
	audioIns { ^this.prIOids( \in, \audio ); }
	controlIns { ^this.prIOids( \in, \control ); }
	audioOuts { ^this.prIOids( \out, \audio ); }
	controlOuts { ^this.prIOids( \out, \control ); }
	
	canFreeSynth { ^this.keys.includes( \u_doneAction ) } // assumes the Udef contains a UEnv
	
	// these may differ in subclasses of Udef
	createSynth { |unit, target, startPos = 0| // create A single synth based on server
		target = target ? Server.default;
		^Synth( this.synthDefName, unit.getArgsFor( target, startPos ), target, \addToTail );
	}
	
	setSynth { |unit ...keyValuePairs|
		// "set % for synths: %".format( keyValuePairs, unit.synths.collect(_.nodeID) ).postln;
		unit.synths.do{ |s|
		    var server = s.server;
		    server.sendSyncedBundle( 0.1, nil, *server.makeBundle( false, {
			    		s.set(*keyValuePairs.clump(2).collect{ |arr| 
			    			[arr[0],arr[1].asControlInputFor(server)] }.flatten)
		    		})
		    	);
		};
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
	
	asUdef { ^this }
		
}

U : ObjectWithArgs {
	
	classvar <>loadDef = false;
	classvar <>synthDict;
	
	// var <def;
	var defName;
	//var <>synths;
	var <>disposeOnFree = true;
	var <>preparedServers;
	var >waitTime; // use only to override waittime from args
	var <>env;
	
	*initClass { synthDict = IdentityDictionary( ) }

	*new { |defName, args|
		^super.new.init( defName, args ? [] )
	}
	
	*defClass { ^Udef }
	
	init { |inName, inArgs|
		var def;
		if( inName.isKindOf( this.class.defClass ) ) {
			def = inName;
			defName = def.name;
			if( defName.isNil ) { defName = def };
		} {
			def = inName.asSymbol.asUdef;
			
		};
		if( def.notNil ) {	
			args = def.asArgsArray( inArgs ? [] )
				.collect({ |item, i|
					if( i.odd ) {
						item.asUnitArg( this );
					} {
						item;
					};
				});
			defName = def.name;
		} { 
			defName = inName;
			"defName '%' not found".format(inName).warn; 
		};
		preparedServers = [];
		env = (); // a place to store things in (for FreeUdef)
		this.changed( \init );
	}
	
	allKeys { ^this.keys }
	allValues { ^this.values }	
	
	set { |...args|
		var synthArgs;
		args.pairsDo({ |key, value|
			value = value.asUnitArg( this );
			this.setArg( key, value );
			synthArgs = synthArgs.addAll( [ key, value ] ); 
		});
		this.def.setSynth( this, *synthArgs );
	}
	
	prSet { |...args| // without changing the arg
		this.def.setSynth( this, *args );
	}
	
	get { |key|
		^this.getArg( key );
	}

	mapSet { |key, value|
		var spec = this.getSpec(key);
		if( spec.notNil ) {
		    this.set(key, spec.map(value) )
		} {
		    this.set(key,value)
		}
	}

	mapGet { |key|
		var spec = this.getSpec(key);
		^if( spec.notNil ) {
		    spec.unmap( this.get(key) )
		} {
		    this.get(key)
		}
	}
	
	release { |releaseTime, doneAction| // only works if def.canFreeSynth == true
		if(releaseTime.isNil, {
			releaseTime = 0.0;
		},{
			releaseTime = -1.0 - releaseTime;
		});
		this.prSet( 
			\u_doneAction, doneAction ?? { this.get( \u_doneAction ) }, 
			\u_gate, releaseTime 
		);
	}
	
	getArgsFor { |server, startPos = 0|
		server = server.asTarget.server;
		^this.args.clump(2).collect({ |item, i|
			[ item[0], switch( item[0], 
				\u_startPos, { startPos },
				\u_dur, { item[1] - startPos },
				\u_fadeIn, { (item[1] - startPos).max(0) },
				{ item[1].asControlInputFor( server, startPos ) }
			) ];
		}).flatten(1);
	}
	
	setAudioIn { |id = 0, bus = 0|
		this.set( this.def.prGetIOKey( \in, \audio, id, "bus" ).asSymbol, bus );
	}
	setControlIn { |id = 0, bus = 0|
		this.set( this.def.prGetIOKey( \in, \control, id, "bus" ).asSymbol, bus );
	}
	setAudioOut { |id = 0, bus = 0|
		this.set( this.def.prGetIOKey( \out, \audio, id, "bus" ).asSymbol, bus );
	}
	setControlOut { |id = 0, bus = 0|
		this.set( this.def.prGetIOKey( \out, \control, id, "bus" ).asSymbol, bus );
	}
	
	getAudioIn { |id = 0, bus = 0|
		^this.get( this.def.prGetIOKey( \in, \audio, id, "bus" ).asSymbol );
	}
	getControlIn { |id = 0, bus = 0|
		^this.get( this.def.prGetIOKey( \in, \control, id, "bus" ).asSymbol );
	}
	getAudioOut { |id = 0, bus = 0|
		^this.get( this.def.prGetIOKey( \out, \audio, id, "bus" ).asSymbol );
	}
	getControlOut { |id = 0, bus = 0|
		^this.get( this.def.prGetIOKey( \out, \control, id, "bus" ).asSymbol );
	}
	
	shouldPlayOn { |target| // this may prevent a unit or chain to play on a specific server 
		^this.def.shouldPlayOn( this, target );
	}
	
	doesNotUnderstand { |selector ...args| 
		// bypasses errors; warning only if arg not found
		if( selector.isSetter ) { 
			this.set( selector.asGetter, *args ) 
		} {
			^this.get( selector );
		};	
	}
	
	// override methods from Object to support args with names 'loop' and 'rate'
	rate { ^this.get( \rate ) }
	rate_ { |new| this.set( \rate, new ) }
	loop { ^this.get( \loop ) }
	loop_ { |new| this.set( \loop, new ) }
	
	def { ^defName.asUdef }
	defName { ^if( defName.class == Symbol ) { defName } { defName.name } }
	
	def_ { |newDef, keepArgs = true|
	  	this.defName_( newDef, keepArgs );
	}

	defName_ { |newName, keepArgs = true|
		this.init( newName, if( keepArgs ) { args } { [] }); // keep args
	}
	
	synths { ^synthDict[ this ] ? [] }
	
	synths_ { |synths| synthDict.put( this, synths ); }
	
	addSynth { |synth|
		 synthDict.put( this, synthDict.at( this ).add( synth ) ); 
	}
	
	removeSynth { |synth|
		var synths;
		synths = this.synths;
		synths.remove( synth );
		if( synths.size == 0 ) {
			 synthDict.put( this, nil ); 
		} {
			 synthDict.put( this, synths ); 
		};
	}

	makeSynth { |target, startPos = 0, synthAction|
		this.def.makeSynth( this, target, startPos, synthAction );
	}
	
	makeBundle { |targets, startPos = 0, synthAction|
		^targets.asCollection.collect({ |target|
			target.asTarget.server.makeBundle( false, {
			    this.makeSynth(target, startPos, synthAction)
			});
		})
	}
	
	start { |target, startPos = 0, latency|
		var targets, bundles;
		target = target ? preparedServers ? Server.default;
		targets = target.asCollection;
		bundles = this.makeBundle( targets );
		latency = latency ? 0.2;
		targets.do({ |target, i|
			if( bundles[i].size > 0 ) {
				target.asTarget.server.sendSyncedBundle( latency, nil, *bundles[i] );
			};
		});
		if( target.size == 0 ) {
			^this.synths[0]
		} { 
			^this.synths;
		};
	}
	
	free { this.synths.do(_.free) } 
	stop { this.free }
	
	resetSynths { this.synths = nil; } // after unexpected server quit
	resetArgs {
		this.values = this.def.values.deepCopy; 
		this.def.setSynth( this, *args );
	}
	
	argSpecs { ^this.def.argSpecs }
	getSpec { |key| ^this.def.getSpec( key ); }

	isPlaying { ^(this.synths.size != 0) }
		
	printOn { arg stream;
		stream << "a " << this.class.name << "(" <<* this.storeArgs  <<")"
	}
	
	getInitArgs {
		var defArgs;
		defArgs = (this.def.args ? []).clump(2);
		^args.clump(2).select({ |item, i| item != defArgs[i] }).flatten(1);
	}
	
	storeArgs { 
		var initArgs;
		initArgs = this.getInitArgs;
		if( initArgs.size > 0 ) {
			^[ this.defName, initArgs ];
		} {
			^[ this.defName ];
		};
	}
	
	asUnit { ^this }

	prSyncCollection { |targets|
        targets.asCollection.do{ |t|
	        t.asTarget.server.sync;
	    };
	}
	
	waitTime { ^(waitTime ?? { this.values.collect( _.u_waitTime ).sum }) + 0.01 }
	
	
	valuesToPrepare {
		^this.values.select( _.respondsTo(\prepare) );
	}
	
	needsPrepare {
		^this.valuesToPrepare.size > 0;
	}
	
	apxCPU { ^this.def.apxCPU }
	
	prepare { |target, startPos = 0, action|
		var valuesToPrepare, act;
		target = target.asCollection.collect{ |t| t.asTarget( this.apxCPU ).server };
		target = target.select({ |tg|
			this.shouldPlayOn( tg ) != false;
		});
		if( target.size > 0 ) {
	   	 act = { preparedServers = preparedServers.addAll( target ); action.value };
		    if( loadDef) {
		        this.def.loadSynthDef( target );
		    };
		    valuesToPrepare = this.valuesToPrepare;
		    if( valuesToPrepare.size > 0  ) {
			    act = MultiActionFunc( act );
			    valuesToPrepare.do({ |val|
				     val.prepare(target.asCollection, startPos, action: act.getAction)
			    });
		    } {
			    act.value; // if no prepare args done immediately
		    };
		} {
			action.value;
		};
	    ^target; // returns targets actually prepared for
    }
    
    prepareAnd { |target, loadDef = true, action|
	    fork{
	        target = this.prepare(target, loadDef);
	        this.prSyncCollection(target);
	        action.value( this );
	    }
    }

	prepareAndStart { |target, loadDef = true|
	   this.prepareAnd( target, loadDef, _.start(target) );
	}

	loadDefAndStart { |target|
	    fork{
	        this.def.loadSynthDef(target.collect{ |t| t.asTarget.server });
	        this.prSyncCollection(target);
	        this.start(target);
	    }
	}

	dispose {
	    this.free;
	    this.values.do{ |val|
	        if(val.respondsTo(\dispose)) {
	            val.dispose
	        }
	    };
	    preparedServers = [];
	}

	disposeArgsFor { |server|
	    this.values.do{ |val|
	        if(val.respondsTo(\disposeFor)) {
	            val.disposeFor(server)
	        }
	    };
	    preparedServers.remove( server );
	}
}

+ Object {
	asControlInputFor { |server, startPos| ^this.asControlInput } // may split between servers
	u_waitTime { ^0 }
	asUnitArg { |unit| ^this }
}

+ Function {
	asControlInputFor { |server, startPos| ^this.value( server, startPos ) }
}

+ Symbol { 
	asUnit { |args| ^U( this, args ) }
	asUdef { |defClass| ^(defClass ? Udef).fromName( this ); }
}

+ Array {
	asUnit { ^U( this[0], *this[1..] ) }
}
