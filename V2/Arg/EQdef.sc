EQdef {
	
	classvar <>global;
	classvar <>dict;
	classvar <>specDict;
	
	var <>names, <>classes, <>argNames, <>defaultSetting, <>specs;
	var <>presets;
	
	*initClass {
		
		Class.initClassTree(ControlSpec);
		
		dict = IdentityDictionary[];
		
		specDict = (
			\freq: [ 20, 20000, \exp, 0, 440 ].asSpec,
			\rq: [ 0.001, 10, \exp, 0.01, 0.707 ].asSpec,
			\rs: [ 0.6, 10, \lin, 0.1, 1 ].asSpec,
			\bw: [ 0.01, 10, \exp, 0.1, 1 ].asSpec,
			\db: [ -36, 36, \lin, 0.25, 0 ].asSpec,
			\a0: [-1,1,\lin, 0, 0 ].asSpec,
			\a1: [-1,1,\lin, 0, 0 ].asSpec,
			\b1: [-1,1,\lin, 0, 0 ].asSpec,
			\b2: [-1,1,\lin, 0, 0 ].asSpec,
			\radius: [ 0,1, \lin,0,0.8].asSpec 	
		);
		
		global = EQdef( 
			'lowShelf', BLowShelf, 
			'peak1', BPeakEQ,
			'peak2', BPeakEQ,
			'peak3', BPeakEQ,
			'hiShelf', BHiShelf
		).defaultSetting_(
			[ 
				[ 100, 1, 0 ], 
				[ 250, 1, 0 ], 
				[ 1000, 1, 0 ], 
				[ 3500, 1, 0 ], 
				[ 6000, 1, 0 ]
			]
		);
		
		dict[ \default ] = global;
	}
	
	*new { |...bandPairs|
		
		// \bandName, Class, \bandName, Class etc..
		// class must respond to *coeffs
		
		^super.newCopyArgs.init( bandPairs );
	}
	
	addToDefs { |key = \new|
		dict[ key ] = this;
	}
	
	*fromKey { |key|
		^dict[ key ]	
	}
	
	init { |bandPairs|
		var methods;
		
		bandPairs = bandPairs.clump(2).flop;
		
		names = bandPairs[0];
		classes = bandPairs[1];
		
		methods = bandPairs[1].collect({ |cl|
			var method;
			method = cl.class.findRespondingMethodFor( \coeffs );
			if( method.isNil ) {
				"%:init - method *coeffs not found for %\n"
					.format( this.class, cl )
					.warn;
			};
			method;
		});
		
		argNames = methods.collect({ |method|
			(method.argNames ? [])[2..].as(Array);
		});
		
		defaultSetting = methods.collect({ |method, i|
			if( method.notNil ) {
				method.prototypeFrame[2 .. argNames[i].size + 1]
			} {
				[]
			};
		});
		
		specs = argNames.collect({ |names|
			names.collect({ |name|
				specDict[ name ];
			});
		});
		
	}
	
	formatSetting { |setting|
		^this.constrainSetting( this.parseSetting( setting ) );
	}
	
	parseSetting { |setting|
		var default;
		default = this.defaultSetting.deepCopy;
		if( setting.isNil ) {
			^default;
		} {
			^default.collect({ |item, i|
				item.collect({ |subItem, ii|
					(setting[i] ? item).asCollection[ii] ? subItem
				});
			});
		};
	}
	
	constrainSetting { |setting|
		// setting needs to be parsed first
		^setting.collect({ |item, i|
			item.collect({ |subItem, ii|
				if( specs[i][ii].notNil ) {
					specs[i][ii].constrain( subItem );
				} {
					subItem;
				};
			});
		});
	}
	
	indexOf { |name, argName|
		var nameIndex, argNameIndex;
		
		if( name.isNumber ) {
			nameIndex = name;
		} {
			nameIndex = names.indexOf( name );
		};
		
		if( nameIndex.isNil ) {
			"%:indexOf - name '%' not found\n"
				.format( this.class, name )
				.warn;
			^[ nil, nil ];
		} {
			if( argName.isNumber ) {
				argNameIndex = argName;
			} {
				argNameIndex = argNames[ nameIndex ].indexOf( argName );
			};
			
			if( argNameIndex.isNil ) {
				^[ nameIndex, nil ];
			} {
				^[ nameIndex, argNameIndex ];
			};
		};
	}
	
	flatIndexOf { |name, argName|
		var argIndex;
		#name, argIndex = this.indexOf( name, argName );
		if( argIndex.notNil ) {
			^(argNames[..name-1].collect(_.size).sum) + argIndex;
		} {
			if( argName.isNil ) {
				^(argNames[..name-1].collect(_.size).sum) + 
					argNames[name].collect({ |item, i| i });
			} {
				^nil;
			};
		};
	}
	
	constrain { |name, argName, value|
		#name, argName = this.indexOf( name, argName );
		if( specs[ name ][ argName ].notNil ) {
			^specs[ name ][ argName ].constrain( value );
		} {
			^value
		};
	}
	
}

EQSetting {
	
	classvar <>global;
	
	var <def, <setting;
	var <>action;
	
	*initClass {
		Class.initClassTree(EQdef);
		global = this.new;
	}
	
	*new { |def, setting|
		^this.newCopyArgs( def, setting ).init;
	}
	
	getEQdef {
		var eqdef;
		
		if( def.isKindOf( EQdef ) ) {
			eqdef = def;
			def = EQdef.dict.findKeyForValue( def );
			if( def.isNil ) { def = eqdef };
		} {
			eqdef = EQdef.dict[ def ];
			if( eqdef.isNil ) { eqdef = EQdef.dict[ \default ] };
		};
		
		^eqdef;
	}
	
	init {
		var eqdef;
		
		def = def ? \default;
		eqdef = this.getEQdef;
		setting = eqdef.formatSetting( setting );		
	}
	
	set { |name, argName, value, constrain = true|
		#name, argName = this.getEQdef.indexOf( name, argName );
		if( argName.notNil ) {
			if( constrain == true ) {
				value = this.getEQdef.constrain( name, argName, value );
			};
			setting[name][argName] = value;
			this.changed( \setting, this.names[name], this.argNames[name][argName], value );
		} {
			if( value.isArray ) {
				this.getEQdef.argNames[ name ].do({ |argName, i|
					if( value[i].notNil ) {
						this.set( name, argName, value[i], constrain );
					};
				});
			};
		};
	}
	
	get { |name, argName|
		var argIndex;
		#name, argIndex = this.getEQdef.indexOf( name, argName );
		if( argIndex.notNil ) {
			^setting[name][argIndex];
		} {
			if( name.notNil ) {
				if( argName.isNil ) {
					^setting[name];
				} {
					^nil;
				};
			} {
				^nil;
			};
		};
	}
	
	at { |name, argName|
		^this.get( name, argName );
	}
	
	put { |...args|
		var value;
		value = args.pop;
		args = args.extend( 2, nil );
		this.set( args[0], args[1], value );
	}
	
	setting_ { |new| 
		setting = this.getEQdef.formatSetting( new );
		this.changed( \setting );
	}
	
	 names { ^this.getEQdef.names }
	 classes { ^this.getEQdef.classes }
	 argNames { ^this.getEQdef.argNames }	
	 defaultSetting { ^this.getEQdef.defaultSetting }
	 specs { ^this.getEQdef.specs }
	 
	 asUGenInput { ^setting.flat.asUGenInput }
	 asControlInput { ^setting.flat.asControlInput }
	 asOSCArgEmbeddedArray { | array| ^setting.flat.asOSCArgEmbeddedArray(array) }
	 
	 ar { |in, setting|
		var eqdef;
	 	setting = setting.flat;
	 	eqdef = this.getEQdef;
		this.classes.do({ |class, i|
			in = class.ar( in, *setting[ eqdef.flatIndexOf( i ) ] );
		});
		^in;
	 }
	 
	 magResponses { |freqs|
		 ^this.classes.collect({ |class, i|
			class.magResponse( freqs ? 1000, 44100, *setting[i] );
		});
	 }

}