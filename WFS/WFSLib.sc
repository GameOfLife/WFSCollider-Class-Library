WFSLib {
	
	*startup { |wfsOptions|
		var defs, servers, o;
		var bootFunc;
		
		this.loadOldPrefs;
		this.loadPrefs;
		
				
		if( WFSSpeakerConf.default.isNil ) {
			WFSSpeakerConf.default = WFSSpeakerConf.fromPreset( \default );
		};
		
		if( wfsOptions.isNil ) {
			wfsOptions = WFSOptions.current ?? { WFSOptions.fromPreset( \default ); };
		};
		
		if( wfsOptions.masterOptions.notNil ) {
			if( wfsOptions.serverOptions.size > 0 ) {
				WFSServers.master( 
					wfsOptions.serverOptions.collect(_.ip),
					wfsOptions.serverOptions.collect(_.startPort),
					wfsOptions.serverOptions[0].n
				)
				.makeDefault
				.hostNames_( *wfsOptions.serverOptions.collect(_.name) );
				
				WFSPathBuffer.writeServers = WFSServers.default.multiServers.collect(_[0]);
			} {
				WFSServers.single( ).makeDefault;
				WFSPathBuffer.writeServers = [ WFSServers.default.m ];
			};
			
			Server.default = WFSServers.default.m;
			o = wfsOptions.masterOptions;
			WFSServers.pulsesOutputBus = o.toServersBus;
			SyncCenter.outBus = o.toServersBus;
			
			if( o.useForWFS ) {
				servers = [ WFSServers.default.m ];
			};
			
		} {
			if( wfsOptions.serverOptions.size > 0 ) {
				
				WFSServers.newCopyArgs( 
					wfsOptions.serverOptions.collect(_.ip),
					wfsOptions.serverOptions.collect(_.startPort),
					wfsOptions.serverOptions[0].n
				).init( false ).makeDefault;
				WFSPathBuffer.writeServers = WFSServers.default.multiServers.collect(_[0]);
				Server.default = WFSServers.default.multiServers[0][0];
				o = wfsOptions.serverOptions[0];
			} {
				"WFSLib:startup : can't startup".postln;
				"\tWFSMasterOptions and WFSServerOptions are missing".postln;
				^nil;
			};
		};
		
		this.setServerOptions( o.numOutputBusChannels );
		Server.default.options.device = o.device;
		Server.default.options.numInputBusChannels = o.numInputBusChannels;
		
		WFS.previewMode = wfsOptions.previewMode;
		
		servers = servers ++ WFSServers.default.multiServers.collect({ |ms|
			LoadBalancer( *ms.servers ) 
		});
		
		WFSSpeakerConf.resetServers;
		
		WFSSpeakerConf.numSystems = servers.size;
		
		servers.do({ |srv, i|
			if( srv.class == LoadBalancer ) {
				srv.servers.do({ |server|
					WFSSpeakerConf.addServer( server, i );
				});
			} {
				WFSSpeakerConf.addServer( srv, i );
			};
		});
		
		if( WFSServers.default.m.notNil && { servers.includes( WFSServers.default.m ).not }) {
			servers = [ WFSServers.default.m ] ++ servers;
		};
		
		Udef.userDefsFolder = File.getcwd +/+ "UnitDefs";
	   
		Udef.defsFolders = Udef.defsFolders.add( 
			WFSArrayPan.filenameSymbol.asString.dirname +/+ "UnitDefs"
		);
			
		UnitRack.defsFolders = UnitRack.defsFolders.add( 
			WFSArrayPan.filenameSymbol.asString.dirname +/+ "UnitRacks"
		);
				
		GlobalPathDict.put( \wfs, "/WFSSoundFiles" );
		GlobalPathDict.put( \resources, String.scDir );
		
		UChain.makeDefaultFunc = {	
			UChain( \bufSoundFile, 
				[ \wfsStaticPoint, 
					[ \point, 5.0.rand2@(5.0 rrand: 10) ] // always behind array
				]
			).useSndFileDur
		};
		
		UChainGUI.presetManager
			.putRaw( \dynamicPoint, { 
				UChain( 
					[ \bufSoundFile, [ 
						\soundFile, BufSndFile.newBasic("@resources/sounds/a11wlk01-44_1.aiff", 
							107520, 1, 44100, 0, nil, 1, true) 
					] ],
					[ \wfsDynamicPoint, 
						[  
							\point, 5.0.rand2@(5.0 rrand: 10),
							\pointLag, 1,
							\quality, \better
						] // always behind array
					]
				).useSndFileDur 
			})
			.putRaw( \staticPlane, { 
				UChain( 
					[ \bufSoundFile, [ 
						\soundFile, BufSndFile.newBasic("@resources/sounds/a11wlk01-44_1.aiff", 
							107520, 1, 44100, 0, nil, 1, true) 
					] ], 
					[ \wfsStaticPlane, [  \point, 5.0.rand2@(5.0 rrand: 10) ] ]
				).useSndFileDur 
			})
			.putRaw( \circlePath, {
				UChain(  
					[ \bufSoundFile, [ 
						\soundFile, BufSndFile.newBasic("@resources/sounds/a11wlk01-44_1.aiff", 
							107520, 1, 44100, 0, nil, 1, true) 
					] ],
					[ \wfsCirclePath, [ \speed, 0.4 ] ],
					[ \wfsDynamicPoint, [ \pointFromBus, true, \quality, \better ] ]
				).useSndFileDur 
			})
			.putRaw( \wfsPath, {
				UChain(  
					[ \bufSoundFile, [ 
						\soundFile, BufSndFile.newBasic("@resources/sounds/a11wlk01-44_1.aiff", 
							107520, 1, 44100, 0, nil, 1, true) 
					] ],
					[ \wfsPathPlayer, [ \wfsPath, 
						WFSPathBuffer( 
							WFSPath2.generate( 10, 5, 
								[ \random, [\seed, 100000.rand, \radius, 10@10] ] 
							), 0, 1, true
						), 
					] ],
					[ \wfsDynamicPoint, [ \pointFromBus, true, \quality, \better ] ]
				).useSndFileDur
			})
			.putRaw( \sinewave, { UChain( 
				\sine,
				[ \wfsDynamicPoint, 
						[  
							\point, 5.0.rand2@(5.0 rrand: 10),
							\pointLag, 1,
							\quality, \better
						] // always behind array
					]
				).useSndFileDur
			})
			.putRaw( \noiseband, { UChain( 
				\pinkNoise,
				[ \cutFilter, [ 
					\freq, 1.0.rand.linexp( 0,1, 200, 2000 ).round(200) + [0,200]
				] ],
				[ \wfsDynamicPoint, [  
					\point, 5.0.rand2@(5.0 rrand: 10),
					\pointLag, 1,
					\quality, \better
				] ]
				).useSndFileDur
			})
			.putRaw( \dualdelay, UChain( 
				'bufSoundFile', 
				[ 'delay', 
					[ 'time', 0.3, 'maxTime', 0.3, 'dry', 0.0, 'amp', 0.5, 'u_o_ar_0_bus', 1 ] 
				],
				[ 'delay', 
					[ 'time', 0.5, 'maxTime', 0.5, 'dry', 0.0, 'amp', 0.5, 'u_o_ar_0_bus', 2 ] 
				], 
				[ 'wfsStaticPoint', [ 'point', Point(-6, 6) ] ], 
				[ 'wfsStaticPlane', [ 'point', Point(6, 6), 'u_i_ar_0_bus', 1 ] ],
				[ 'wfsStaticPlane', [ 'point', Point(-6, -6), 'u_i_ar_0_bus', 2 ] ]
				)
			);
		
		defs = Udef.loadAllFromDefaultDirectory.collect(_.synthDef).flat.select(_.notNil);
		UnitRack.loadAllFromDefaultDirectory;
		
		defs.do({|def| def.writeDefFile; });
		if( SyncCenter.mode == 'sample' ) {
			SyncCenter.writeDefs;
		};
		
		ULib.servers = servers;
		
		if( wfsOptions.showServerWindow ) {
			WFSServers.default.makeWindow;
		};
		
		if( wfsOptions.showGUI ) {
			
			if(thisProcess.platform.class.asSymbol === 'OSXPlatform' && {
					thisProcess.platform.ideName.asSymbol === \scapp 
				}) {
			    UMenuBar();
			    SCMenuItem.new(UMenuBar.viewMenu, "WFSSpeakerConfGUI").action_({
					WFSSpeakerConfGUI.newOrCurrent;
				});
			    SCMenuItem.new(UMenuBar.viewMenu, "WFSPositionTracker").action_({
					WFSPositionTrackerGUI.newOrCurrent;
				});
			} {
				UMenuWindow();
				UMenuWindow.viewMenu.tree.put( 'WFSSpeakerConfGUI', {
					WFSSpeakerConfGUI.newOrCurrent;
				});
				UMenuWindow.viewMenu.tree.put( 'WFSPositionTracker', {
					WFSPositionTrackerGUI.newOrCurrent;
				});
			};
	
			UGlobalGain.gui;
			UGlobalEQ.gui;
		};
		
	  WFSSynthDefs.generateAllOnce({
	  	WFSServers.default.boot;
	  });
	  
	  if( wfsOptions.playSoundWhenReady ) {
		  Routine({
            		var allTypes, defs;
	              while { 
		            	WFSServers.default.multiServers.collect(_.servers)
		            		.flatten(1).collect( _.serverRunning ).every( _ == true ).not; 
		         } { 
			          0.2.wait; 
			    };
	             "System ready; playing lifesign".postln;
	             "server ready".speak
		   }).play( AppClock );
	  };
	  		 
	}
	
	*setServerOptions{ |numOuts=96|
		Server.default.options
			.numPrivateAudioBusChannels_(256)
			.numOutputBusChannels_(numOuts)
			.numInputBusChannels_(20)
			.numWireBufs_(2048)
			.memSize_(2**19) // 256MB
			.hardwareBufferSize_(512)
			.blockSize_(128)
			.sampleRate_( 44100 )
			.maxNodes_( 2**16 );
     }

	*getCurrentPrefsPath { |action|
		var paths;
		paths = [
			File.getcwd,
			"/Library/Application Support/WFSCollider",
			"~/Library/Application Support/WFSCollider".spath
		].collect(_ +/+ "preferences.scd");
		
		paths.do({ |path|
			if( File.exists( path ) ) {
				action.value( path );
				^path;
			};
		});
		
		^nil;
	}
	
	*loadPrefs {
		this.getCurrentPrefsPath(_.load);
	}
	
	*openPrefs {
		this.getCurrentPrefsPath(Document.open(_));
	}
	
	*formatPrefs {
		var stream;
		stream = CollStream();
		
		stream << "//// WFSCollider preferences (generated on: %) ////\n\n"
			.format( Date.localtime.asString );
			
		stream << "//speaker configuration:\n";
		stream <<< WFSSpeakerConf.default << ".makeDefault;\n\n";
		
		stream << "//options:\n";
		stream <<< WFSOptions.current << ";";
		
		if( WFSArrayPan.useFocusFades != true ) {
			stream << "\n\nWFSArrayPan.useFocusFades = " << WFSArrayPan.useFocusFades << ";";
		};
		if( WFSArrayPan.tapering != 0 ) {
			stream << "\n\nWFSArrayPan.tapering = " <<< WFSArrayPan.tapering << ";";
		};
		
		^stream.collection;
	}

	*writePrefs { |path|
		var file;
		path = path ? this.getCurrentPrefsPath ? 
			"~/Library/Application Support/WFSCollider/preferences.scd".spath;
		path.dirname.makeDir;
		"writing preferences file:\n%\n".postf( path );
		file = File( path, "w" );
		file.write( this.formatPrefs );
		file.close;
	}
	
	*loadOldPrefs {
		var file, dict;
		if( File.exists( 
			"/Library/Application Support/WFSCollider/WFSCollider_configuration.txt" 
		) ) {
			file = File(
				"/Library/Application Support/WFSCollider/WFSCollider_configuration.txt","r"
			);
			dict = file.readAllString.interpret;
			file.close;
			
			WFSSpeakerConf.rect( *dict[\speakConf][[0,1,3,2]] * [1,1,0.5,0.5] ).makeDefault;
			
			if(dict[\hostname].notNil){
				"starting server mode".postln;
				WFSOptions.fromPreset( 'game_of_life_server' );
			};
			
			if(dict[\ips].notNil){
				"starting client mode".postln;
				WFSOptions()
					.masterOptions_(
						WFSMasterOptions()
							.toServersBus_(14)
							.numOutputBusChannels_(20)
							.device_( dict[\soundCard] ? "MOTU 828mk2" )					)
					.serverOptions_(
						dict[ \ips ].collect({ |ip, i|
							var startport;
							if( dict[\startPorts].notNil ) {
								startport = dict[\startPorts].asCollection.wrapAt(i);
							};
							WFSServerOptions()
								.ip_( ip )
								.n_( dict[\scsynthsPerSystem] ? 8 )
								.startPort_(  startport ? 58000 )
								.name_( dict[ \hostnames ][i] );
						})
					);
			};	
		};	
	}
	
}