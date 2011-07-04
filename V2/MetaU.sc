/*

(
a = MetaUdef(\copy,{ |numChannels|
    { |outBus = 0|
        Out.ar(0, UIn.ar(0,numChannels) )
    }
},[\numChannels,1],[\outBus,0],[\outBus,[0,300,1].asSpec]);
b = Udef(\noise,{ UOut.ar(0,WhiteNoise.ar.dup(4) * 0.1 ) });
x = MetaU(\copy,[\numChannels,1],[\outBus,0]);
z = UChain(\noise,x);
)

z.prepareAndStart(s)

x.setMeta(\numChannels,2)
// this will stop the running synth, send new synthDef and startSynthAgain.

Setting the specs for Unit arguments:

a = MetaUdef(\copy,{ |numChannels|
    { |outBus = 0|
        Out.ar(0, UIn.ar(0,numChannels) )
    }
},[\numChannels,1],[\outBus,[0,300,1].asSpec]);

it can also be a function that depends on the meta args

a = MetaUdef(\copy,{ |numChannels|
    { |outBus = 0|
        Out.ar(0, UIn.ar(0,numChannels) )
    }
},[\numChannels,1],{ |numChannels| [\outBus,[0,numChannels*20,1].asSpec] });

*/

MetaUdef : GenericDef {

	classvar <>all,<>defsFolder;

	var <>func, <>category;
	var <>udefArgsFunc;

	*initClass{
		defsFolder = this.filenameSymbol.asString.dirname.dirname +/+ "MetaUnitDefs";
	}

	*new { |name, func, args, udefArgsFunc, category|
		^super.new( name, args ).init( func, udefArgsFunc ).category_( category ? \default );
	}

	init { |inFunc, inUdefArgsFunc|
		func = inFunc;
		argSpecs = ArgSpec.fromFunc(func,argSpecs);
		udefArgsFunc = inUdefArgsFunc
	}

	createUnit { |metaUnit,unitArgs|
	    var name = this.name++"_"++metaUnit.args.reduce{ |a,b| a.asString++"_"++b.asString };
	    Udef(name, func.value(*metaUnit.values), udefArgsFunc.value(*metaUnit.values) );
	    ^U(name, metaUnit.unitArgs)
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


MetaU : ObjectWithArgs {

    var <def;
    var <unitArgs;
    var <unit;

    *new { |defName, args, unitArgs|
        ^super.new.init( defName, args ? [], unitArgs ? []);
    }

    *defClass { ^MetaUdef }

    init { |inName, inArgs, inUnitArgs|
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
        unitArgs = inUnitArgs;
        this.makeUnit;
    }

    defName_ { |name, keepArgs = true|
        this.init( name.asSymbol, if( keepArgs ) { args } { [] }); // keep args
    }

    defName { ^def !? { def.name } }

    makeUnit{
        unit = def.createUnit(this,unitArgs)
    }

    setMeta { |key, value|
        var isPlaying, targets;
		this.setArg( key, value );
		isPlaying = unit.isPlaying;
		targets = unit.synths.collect(_.group);
		unit.free;
		this.makeUnit;
		if(isPlaying) {
		    unit.start(targets)
		}
	}

	getMeta { |key|
		^this.getArg( key );
	}

    // foward to unit
    set { |key, value| unit.set(key,value) }
    get { |key| ^unit.get(key) }
    mapSet { |key, value| unit.mapSet(key,value) }
    mapGet { |key| ^unit.mapGet(key) }
    getArgsFor { |server| ^unit.getArgsFor }
    makeSynth {|target, synthAction| ^unit.makeSynth(target,synthAction) }
	makeBundle { |targets, synthAction| ^unit.makeBundle(targets, synthAction) }
	start { |target, latency| ^unit.start(target, latency) }
	free { unit.free }
	stop { unit.stop }
	resetSynths { unit.resetSynths }
	resetArgs { unit.resetArgs }
	asUnit { ^this }
	disposeOnFree_{ |bool| unit.disposeOnFree_(bool) }

    prepare { |server|
        unit.def.loadSynthDef;
        unit.prepare(server);
    }
    prepareAndStart{ |server|
        unit.def.loadSynthDef;
        unit.prepareAndStart(server);
    }
    dispose {
        unit.dispose()
    }
    disposeArgsFor { |server|
        unit.disposeArgsFor(server)
    }

    printOn { arg stream;
        stream << "a " << this.class.name << "(" <<* [this.defName, args,unitArgs]  <<")"
    }

    storeOn { arg stream;
        stream << this.class.name << "(" <<* [
            ( this.defName ? this.def ).asCompileString,
            args.asCompileString,
            unitArgs.asCompileString
        ]  <<")"
    }
}
