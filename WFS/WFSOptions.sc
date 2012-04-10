WFSMultiServerOptions {
	
	classvar <>presets;
	
	var <>name = "Game Of Life 1";
	var <>ip = "127.0.0.1";
	var <>startPort = 58000;
	var <>n = 8;
	var <>numOutputBusChannels = 96;
	var <>numInputBusChannels = 8;
	var <>device;
	
	*initClass {
		presets = ( 
			'game_of_life_1': WFSMultiServerOptions()
				.name_( "Game Of Life 1" )
				.ip_( "192.168.2.11" )
				.startPort_( 58000 )
				.n_( 8 )
				.numOutputBusChannels_( 96 )
				.numOutputBusChannels_( 8 )
				.device_( "JackRouter" ),
			'game_of_life_2': WFSMultiServerOptions()
				.name_( "Game Of Life 2" )
				.ip_( "192.168.2.12" )
				.startPort_( 58000 )
				.n_( 8 )
				.numOutputBusChannels_( 96 )
				.numInputBusChannels_( 8 )
				.device_( "JackRouter" ),
			'sampl': WFSMultiServerOptions()
				.name_( "SamPL WFS" )
				.ip_( "127.0.0.1" )
				.startPort_( 58000 )
				.n_( 4 )
				.numOutputBusChannels_( 32 )
				.numInputBusChannels_( 32 )
				.device_( nil ),
			'bea7': WFSMultiServerOptions()
				.name_( "BEA7 WFS" )
				.ip_( "127.0.0.1" )
				.startPort_( 58000 )
				.n_( 6 )
				.numOutputBusChannels_( 128 )
				.numInputBusChannels_( 128 )
				.device_( nil )
		);	
	}
	
	*fromPreset { |name| ^presets[ name ]; }
	
	storeModifiersOn { |stream|
		
	}
	
}

WFSMasterOptions {
	
	var <>toServersBus = 14;
	var <>numOutputBusChannels = 20;
	var <>numInputBusChannels = 20;
	var <>device;
	
}

WFSOptions {
	
	classvar <>presets;
	classvar <>current;
	
	var <>masterOptions;
	var <>serverOptions = #[];
	
	*new { ^super.new.init; }
	
	init {
		current = this;
	}
	
	*initClass {
		Class.initClassTree( WFSMultiServerOptions );
		presets = ( 
			'game_of_life_master': WFSOptions()
				.masterOptions_(
					WFSMasterOptions()
						.toServersBus_(14)
						.numOutputBusChannels_(20)
						.device_( "MOTU 828mk2" )					)
				.serverOptions_([	
					WFSMultiServerOptions.fromPreset( 'game_of_life_1' ),
					WFSMultiServerOptions.fromPreset( 'game_of_life_2' )
				]),
			'sampl': WFSOptions()
				.serverOptions_([
					WFSMultiServerOptions.fromPreset( 'sampl' )
				]),
			'bea7_client':  WFSOptions()
				.serverOptions_([	
					WFSMultiServerOptions.fromPreset( 'bea7' )
						.ip_( "192.168.2.11" ) // ?
				]),
			'bea7_server':  WFSOptions()
				.serverOptions_([	
					WFSMultiServerOptions.fromPreset( 'bea7' )
				])
		);	
	}
	
	*fromPreset { |name| ^presets[ name ]; }
	
	storeModifiersOn { |stream|
		
	}
	
}