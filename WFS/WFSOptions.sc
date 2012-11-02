AbstractWFSOptions {
	
	classvar <>usePresetsForCS = true;
	
	*fromPreset { |name| ^this.presets[ name ].copy; }
	
	== { |that| // use === for identity
		^this.compareObject(that);
	}
	
	doesNotUnderstand { |selector, arg1 ...args|
		// MVC support for all args that have no setters
		if( selector.isSetter ) {
			selector = selector.asGetter;
			if( this.class.instVarNames.includes( selector ) ) {
				this.slotPut( selector, arg1 );
				this.changed( selector, arg1 );
			};
		};
	}
	
	storeOn { arg stream;
		stream << this.class.name;
		this.storeModifiersOn(stream);
	}
	
	storeModifiersOn { |stream|
		var preset;
		preset = this.class.presets.findKeyForValue(this);
		if( usePresetsForCS && { preset.notNil } ) {
			stream << ".fromPreset(" <<< preset << ")";
		} {	
			stream << "()";
			this.class.instVarNames.do({ |item, i|
				if( this.perform( item ) != this.class.iprototype[i] ) {
					stream << "\n\t." << item << "_(" <<< this.perform( item ) << ")";
				};
			});
		}
	}
	
}

WFSMasterOptions : AbstractWFSOptions {

	var <toServersBus = 14;
	var <numOutputBusChannels = 20;
	var <numInputBusChannels = 20;
	var <device;
	var <useForWFS = false;
	
	*presets { ^Dictionary[] }
		
}

WFSServerOptions : AbstractWFSOptions {
	
	classvar <>presets;
	
	var <name = "Game Of Life 1";
	var <ip = "127.0.0.1";
	var <startPort = 58000;
	var <n = 8;
	var <numOutputBusChannels = 96;
	var <numInputBusChannels = 8;
	var <device = "JackRouter";
	
	
	*initClass {
		presets = Dictionary[
			'game_of_life_1'-> WFSServerOptions()
				.name_( "Game Of Life 1" )
				.ip_( "192.168.2.11" ),
			'game_of_life_2'-> WFSServerOptions()
				.name_( "Game Of Life 2" )
				.ip_( "192.168.2.12" ),
			'sampl'-> WFSServerOptions()
				.name_( "SamPL WFS" )
				.ip_( "127.0.0.1" )
				.n_( 4 )
				.numOutputBusChannels_( 32 )
				.numInputBusChannels_( 32 )
				.device_( "PreSonus FireStudio" ),
			'bea7'-> WFSServerOptions()
				.name_( "BEA7 WFS" )
				.ip_( "127.0.0.1" )
				.n_( 6 )
				.numOutputBusChannels_( 128 )
				.numInputBusChannels_( 128 )
				.device_( nil ) // ?
		];	
	}
	
	useForWFS { ^true }
	
}

WFSOptions : AbstractWFSOptions {
	
	classvar <>presets;
	classvar <>current;
	
	var <masterOptions;
	var <serverOptions = #[];
	var <showGUI = true;
	var <showServerWindow = true;
	var <previewMode = nil;
	var <playSoundWhenReady = false;
	
	*new { ^super.new.init; }
	
	init {
		current = this;
	}
	
	*fromPreset { |name| ^this.presets[ name ].copy.init; }
	
	*initClass {
		Class.initClassTree( WFSServerOptions );
		presets = Dictionary[
			'default'-> WFSOptions() // offline
				.masterOptions_(
					WFSMasterOptions()
						.useForWFS_(true)
				)
				.previewMode_( \headphone ),
			'game_of_life_master'-> WFSOptions()
				.masterOptions_(
					WFSMasterOptions()
						.toServersBus_(14)
						.numOutputBusChannels_(20)
						.device_( "MOTU 828mk2" )					)
				.serverOptions_([	
					WFSServerOptions.fromPreset( 'game_of_life_1' ),
					WFSServerOptions.fromPreset( 'game_of_life_2' )
				]),
			'game_of_life_server'-> WFSOptions()
				.serverOptions_([	
					WFSServerOptions()
				])
				.showGUI_( false )
				.playSoundWhenReady_( true ),
			'sampl'-> WFSOptions()
				.serverOptions_([
					WFSServerOptions.fromPreset( 'sampl' )
				]),
			'bea7_client'->  WFSOptions()
				.serverOptions_([	
					WFSServerOptions.fromPreset( 'bea7' )
						.ip_( "192.168.2.11" ) // ?
				]),
			'bea7_server'->  WFSOptions()
				.serverOptions_([	
					WFSServerOptions.fromPreset( 'bea7' )
				])
				.showGUI_( false )
		];
		current = nil;
	}	
}