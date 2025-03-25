WFSLib {

	classvar <previewMode, <previewModeCtrl;


	*startup { |wfsOptions|
		var servers, wfsServers, ms, o;
		var bootFunc, speakerConfBackup;
		var buildArrayPanners = false;

		if( Platform.ideName == "scapp" ) { ^this.startupOld( wfsOptions ) };

		if( Server.respondsTo( \nodeAllocClass_ ) ) {
			Server.nodeAllocClass = UNodeIDAllocator;
		};

		WFSOptions.presetManager.filePath = Platform.userConfigDir +/+ "default" ++ "." ++ WFSOptions.presetManager.id ++ ".presets";
		WFSOptions.presetManager.readAdd( silent: true );

		WFSOptions.makeCurrentAtInit = true;

		speakerConfBackup = WFSSpeakerConf.default;

		this.loadOldPrefs;
		this.loadPrefs;

		ULib.closeServers;

		if( WFSSpeakerConf.default.isNil ) {
			WFSSpeakerConf.default = WFSSpeakerConf.fromPreset( \default );
		};

		if( wfsOptions.isNil ) {
			wfsOptions = WFSOptions.current ?? { WFSOptions.fromPreset( \default ); };
		} {
			WFSSpeakerConf.default = speakerConfBackup ? WFSSpeakerConf.default;
		};

		wfsOptions.makeCurrent;

		WFSOptions.makeCurrentAtInit = false;

		wfsOptions.removeMasterOptions;

		wfsOptions.serverOptions.do({ |so,i|
			var lb;
			if( so.n == 1 ) {
				lb = Server( so.name, NetAddr( so.ip, so.startPort ) );
			} {
				lb = LoadBalancer.fill( so.n, so.name.split($ ).collect(_[0]).join.toLower, NetAddr( so.ip, so.startPort ) );
				lb.name = so.name;
			};
			servers = servers.add( lb );

			lb.options
			.numInputBusChannels_( so.numInputBusChannels )
			.numOutputBusChannels_( so.numOutputBusChannels )
			.inDevice_( so.inDevice )
			.outDevice_( so.outDevice )
			.hardwareBufferSize_( so.hardwareBufferSize )
			.blockSize_( wfsOptions.blockSize )
			.sampleRate_( wfsOptions.sampleRate )
			.maxSynthDefs_(4096)
		    .numWireBufs_(2048)
			.memSize_( (2**19).asInteger ) // 256MB
			.maxNodes_( (2**16).asInteger );

			if( lb.options.respondsTo( \maxLogins ) ) {
				lb.options
				.maxLogins_(2)
				.bindAddress_("0.0.0.0");
			};

			this.setOutputBusStartOffset( lb, so.outputBusStartOffset );
			if( so.useForWFS ) { wfsServers = wfsServers.add( lb ); };
		});

		WFSLib.previewMode = wfsOptions.previewMode;

		UEvent.renderNumChannels = {
			var num;
			UGen.buildSynthDef = SynthDef("temp", {});
			num = WFSPreviewSynthDefs.pannerFuncs[ \n ][ WFSLib.previewMode ].value(0,0@0) !?
				{ |x| x.asArray.size };
			UGen.buildSynthDef = nil;
			num = num ?? {
				WFSSpeakerConf.default.getArraysFor(
					ULib.servers[0].asTarget.server
				).collect(_.n).sum
			};
			if( num == 0 ) {
				SCAlert( "Can't export audio file with current setting. Please try again with a different previewMode.",
					[ "open prefs", "ok" ],
					[ { WFSOptionsGUI.newOrCurrent }, { } ]
				);
			};
			num;
		};

		WFSSpeakerConf.resetServers;
		WFSSpeakerConf.numSystems = wfsServers.size;
		wfsServers.do({ |srv, i| WFSSpeakerConf.addServer( srv, i ); });

		Udef.userDefsFolder = File.getcwd +/+ "UnitDefs";
		UMapDef.userDefsFolder = File.getcwd +/+ "UMapDefs";

		[ Udef, "UnitDefs", UMapDef, "UMapDefs", UnitRack, "UnitRacks" ].pairsDo({ |a,b|
			a.defsFolders = a.defsFolders.add( WFSArrayPan.filenameSymbol.asString.dirname +/+ b );
		});

		GlobalPathDict.put( \wfs, WFSOptions.current.wfsSoundFilesLocation );

		if( SyncCenter.mode == 'sample' ) {
			SyncCenter.writeDefs;
		};

		Udef.synthDefDir = Platform.userAppSupportDir +/+ "u_synthdefs/";

		if( Udef.synthDefDir.notNil ) { File.mkdir( Udef.synthDefDir ); };

		ULib.loadUDefs( false );

		this.setGUISkin( WFSOptions.current.skin ? \light, false );

		if( WFSOptions.current.showGUI ) { this.initDefaults };

		ULib.servers = servers;

		WFSPathBuffer.writeServers = ULib.servers.collect({ |srv|
			if( srv.isKindOf( LoadBalancer ) ) {
				srv[0];
			} {
				srv;
			};
		});

		ULib.envirSpecs = [
			'value', [0,1].asSpec,
			'freq', FreqSpec(2,20000),
			'amp', \amp.asSpec,
			'integer', IntegerSpec(),
			'boolean', BoolSpec(),
			'time', SMPTESpec(),
			'point', WFSPointSpec(200),
		];

		UScore.openFunc = { |path| // old xml format compatibility
			if( File(path,"r").readAllString[..8] == "<xml:wfs>") {
				WFSScore.readWFSFile(path).asUEvent;
			} {
				UScore.readTextArchive( path );
			};
		};

		if( wfsOptions.showGUI ) { this.initGUI };

		if( wfsOptions.showServerWindow ) {
			ULib.serversWindow( "WFSCollider Servers" );
		};

		wfsOptions.startupAction.value( this );

		File.mkdir( Platform.userAppSupportDir +/+ "wfs_synthdefs" );

		buildArrayPanners = (WFSLib.previewMode == \off);

		WFSSynthDefs.generateAllOrCopyFromResources({
			StartUp.defer({
				{ ULib.servers.do(_.bootSync); }.fork( AppClock );
			})
		}, Platform.userAppSupportDir +/+ "wfs_synthdefs", true, buildArrayPanners );

		UEvent.nrtStartBundle = [ [ "/d_loadDir", Platform.userAppSupportDir +/+ "wfs_synthdefs" ] ];

		if( Udef.synthDefDir.notNil ) {
			UEvent.nrtStartBundle = UEvent.nrtStartBundle.add( [ "/d_loadDir", Udef.synthDefDir ] )
		};

		ServerBoot.add( this );

		CmdPeriod.add( this );

		if( wfsOptions.playSoundWhenReady or: { wfsOptions.serverAction.notNil } ) {
			Routine({
				var allTypes, defs;
				var servers;
				servers = ULib.allServers;
				while {
					servers.collect( _.serverRunning ).every( _ == true ).not;
				} {
					0.2.wait;
				};
				"System ready".postln;
				if( wfsOptions.playSoundWhenReady ) {
					"playing lifesign".postln;
					"say 'server %, ready'".format( ULib.servers.first.name ).unixCmd;
				};
				servers.do({ |srv| wfsOptions.serverAction.value( srv ) });
			}).play( AppClock );
		};

		Server.default = ULib.allServers.first;

		ULib.allServers.do({ |srv|
			NotificationCenter.register(srv, \newAllocators, \ulib, {
				// Substitute anything more meaningful here:
				srv.audioBusAllocator.uReserve( srv.options.firstPrivateBus, 64 );
				srv.controlBusAllocator.uReserve( 1500, 500 );
				srv.controlBusAllocator.uReserve( 2000, 8 );
				"WFSLib.startup: reserved audio and control buses".postln;
			});
		});

		Document.initAction = { |doc|
			if( doc.path !? { |x| x.split( $. ).last == "uscore" } ? false ) {
				UScore.open( doc.path, _.gui );
				doc.close;
			};
		};
	}

	*previewMode_ { |pm| previewMode = pm; this.changed( \previewMode, previewMode ); }

	*makePreviewModeCtrl {
		previewModeCtrl.remove;
		previewModeCtrl = SimpleController( this ).put( \previewMode, {
			{ UChainGUI.all.do({ |item| item.chain.changed( \units ) }); }.defer;
		});
	}

	*startupOld { |wfsOptions, useMenuWindow = false|
		var servers, o;
		var bootFunc;

		if( Server.respondsTo( \nodeAllocClass_ ) ) {
			Server.nodeAllocClass = UNodeIDAllocator;
		};

		WFSOptions.presetManager.filePath = Platform.userConfigDir +/+ "default" ++ "." ++ WFSOptions.presetManager.id ++ ".presets";
		WFSOptions.presetManager.readAdd( silent: true );

		WFSOptions.makeCurrentAtInit = true;

		this.loadOldPrefs;
		this.loadPrefs;

		WFSServers.default !? _.close;

		if( WFSSpeakerConf.default.isNil ) {
			WFSSpeakerConf.default = WFSSpeakerConf.fromPreset( \default );
		};

		if( wfsOptions.isNil ) {
			wfsOptions = WFSOptions.current ?? { WFSOptions.fromPreset( \default ); };
		};

		wfsOptions.makeCurrent;

		WFSOptions.makeCurrentAtInit = false;

		if( wfsOptions.masterOptions.notNil ) {
			if( wfsOptions.serverOptions.size > 0 ) {
				WFSServers.master(
					wfsOptions.serverOptions.collect(_.ip),
					wfsOptions.serverOptions.collect(_.startPort),
					wfsOptions.serverOptions[0].n
				)
				.makeDefault
				.hostNames_( *wfsOptions.serverOptions.collect(_.name) );
				wfsOptions.serverOptions.do({ |item, i|
					WFSServers.default[ 0 ][ i ].options						.numInputBusChannels_(  item.numInputBusChannels )
						.numOutputBusChannels_( item.numOutputBusChannels )
						.blockSize_( wfsOptions.blockSize )
						.sampleRate_( wfsOptions.sampleRate )
						.maxSynthDefs_(2048)
					    .numWireBufs_(2048)
						.hardwareBufferSize_( item.hardwareBufferSize );
					if( item.device.isString or: item.device.isNil ) {
						WFSServers.default[ 0 ][ i ].options.device = item.device;
					} {
						WFSServers.default[ 0 ][ i ].options.inDevice = item.device[0];
						WFSServers.default[ 0 ][ i ].options.outDevice = item.device[1];
					};
					if( WFSServers.default[ 0 ][ i ].options.respondsTo( \maxLogins ) ) {
						WFSServers.default[ 0 ][ i ].options.maxLogins_(2);
						WFSServers.default[ 0 ][ i ].options.bindAddress_("0.0.0.0");
					};
					 WFSServers.default.multiServers[i].servers.do({ |srv|
						 WFSSpeakerConf.setOutputBusStartOffset( srv, item.outputBusStartOffset );
					 });
				});
				WFSPathBuffer.writeServers = WFSServers.default.multiServers.collect(_[0]);
			} {
				WFSServers.single( ).makeDefault;
				WFSPathBuffer.writeServers = [ WFSServers.default.m ];
			};

			Server.default = WFSServers.default.m;
			o = wfsOptions.masterOptions;
			WFSServers.pulsesOutputBus = o.toServersBus;
			SyncCenter.outBus = o.toServersBus;
			WFSSpeakerConf.setOutputBusStartOffset( WFSServers.default.m, o.outputBusStartOffset );

			if( o.useForWFS ) {
				servers = [ WFSServers.default.m ];
			};

		} {
			if( wfsOptions.serverOptions.size > 0 ) {

				WFSServers.newCopyArgs(
					wfsOptions.serverOptions.collect(_.ip),
					wfsOptions.serverOptions.collect(_.startPort),
					wfsOptions.serverOptions[0].n
				).init( false )
					.makeDefault
					.hostNames_( *wfsOptions.serverOptions.collect(_.name) );
				WFSPathBuffer.writeServers = WFSServers.default.multiServers.collect(_[0]);
				Server.default = WFSServers.default.multiServers[0][0];
				o = wfsOptions.serverOptions[0];
				wfsOptions.serverOptions.do({ |item,i|
					if( WFSServers.default[ 0 ][ i ].options.respondsTo( \maxLogins ) ) {
						WFSServers.default[ 0 ][ i ].options.maxLogins_(2);
						WFSServers.default[ 0 ][ i ].options.bindAddress_("0.0.0.0");
					};
					WFSServers.default.multiServers[i].servers.do({ |srv|
						 WFSSpeakerConf.setOutputBusStartOffset( srv, item.outputBusStartOffset );
					});
				});
			} {
				"WFSLib:startup : can't startup".postln;
				"\tWFSMasterOptions and WFSServerOptions are missing".postln;
				^nil;
			};
		};

		this.setServerOptions( o.numOutputBusChannels, o.numInputBusChannels, 64 );
		Server.default.options.blockSize = wfsOptions.blockSize;
		Server.default.options.sampleRate = wfsOptions.sampleRate;
		if( o.device.isString or: o.device.isNil ) {
			Server.default.options.device = o.device;
		} {
			Server.default.options.inDevice = o.device[0];
			Server.default.options.outDevice = o.device[1];
		};
		Server.default.options.hardwareBufferSize = o.hardwareBufferSize;


		WFSLib.previewMode = wfsOptions.previewMode;

		UEvent.renderNumChannels = {
			var num;
			num = WFSPreviewSynthDefs.pannerFuncs[ \n ][ WFSLib.previewMode ].value(0,0@0) !?
				{ |x| x.asArray.size };
			num = num ?? {
				WFSSpeakerConf.default.getArraysFor(
					ULib.servers[0].asTarget.server
				).collect(_.n).sum
			};
			if( num == 0 ) {
				SCAlert( "Can't export audio file with current setting. Please try again with a different previewMode.", [ "open prefs", "ok" ], [ { WFSOptionsGUI.newOrCurrent }, { } ] );
			};
			num;
		};

		servers = servers ++ WFSServers.default.multiServers.collect({ |ms|
			if( ms.servers.size > 1 ) {
				LoadBalancer( *ms.servers ).name_( ms.hostName.asSymbol )
			} {
				ms.servers[0];
			};
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
		UMapDef.userDefsFolder = File.getcwd +/+ "UMapDefs";

		Udef.defsFolders = Udef.defsFolders.add(
			WFSArrayPan.filenameSymbol.asString.dirname +/+ "UnitDefs"
		);

		UMapDef.defsFolders = UMapDef.defsFolders.add(
			WFSArrayPan.filenameSymbol.asString.dirname +/+ "UMapDefs"
		);

		UnitRack.defsFolders = UnitRack.defsFolders.add(
			WFSArrayPan.filenameSymbol.asString.dirname +/+ "UnitRacks"
		);

		GlobalPathDict.put( \wfs, WFSOptions.current.wfsSoundFilesLocation );

		if( SyncCenter.mode == 'sample' ) {
			SyncCenter.writeDefs;
		};

		Udef.synthDefDir = Platform.userAppSupportDir +/+ "u_synthdefs/";

		if( Udef.synthDefDir.notNil ) { File.mkdir( Udef.synthDefDir ); };

		ULib.loadUDefs( false );

		if( WFSOptions.current.showGUI ) { this.initDefaults };

		ULib.servers = servers;

		if( wfsOptions.showServerWindow ) {
			WFSServers.default.makeWindow;
		};

		UMenuBar.remove;
		if( thisProcess.platform.class.asSymbol === 'OSXPlatform' && {
			Platform.ideName == "scapp";
		} ) {
			thisProcess.preferencesAction = { WFSOptionsGUI.newOrCurrent; };
		};

		UScore.openFunc = { |path|
			if( File(path,"r").readAllString[..8] == "<xml:wfs>") {
				WFSScore.readWFSFile(path).asUEvent;
			} {
				UScore.readTextArchive( path );
			};
		};

		if( wfsOptions.showGUI ) { this.initGUI( useMenuWindow ) };

	  wfsOptions.startupAction.value( this );

	  File.mkdir( Platform.userAppSupportDir +/+ "wfs_synthdefs" );

	  WFSSynthDefs.generateAllOrCopyFromResources({
	  	StartUp.defer({
			ULib.servers.do(_.boot);
		})
	  }, Platform.userAppSupportDir +/+ "wfs_synthdefs" );

      UEvent.nrtStartBundle = [ [ "/d_loadDir", Platform.userAppSupportDir +/+ "wfs_synthdefs" ] ];
		if( Udef.synthDefDir.notNil ) { UEvent.nrtStartBundle = UEvent.nrtStartBundle.add( [ "/d_loadDir", Udef.synthDefDir ] ) };

	  ServerBoot.add( this );

	  CmdPeriod.add( this );

	  if( wfsOptions.playSoundWhenReady or: { wfsOptions.serverAction.notNil } ) {
		  Routine({
            		var allTypes, defs;
            		var servers;
            		servers = WFSServers.default.multiServers.collect(_.servers).flatten(1);
            		if( WFSServers.default.m.notNil ) {
	            		servers = servers ++ WFSServers.default.m;
            		};
	              while {
		            	servers.collect( _.serverRunning ).every( _ == true ).not;
		         } {
			          0.2.wait;
			    };
	             "System ready".postln;
	             if( wfsOptions.playSoundWhenReady ) {
		             "playing lifesign".postln;
					"say 'server %, ready'"
		             	.format(
		             		WFSServers.default.multiServers.collect(_.hostName).join( ", ")
		             	).unixCmd;
	             };
	             servers.do({ |srv| wfsOptions.serverAction.value( srv ) });
		   }).play( AppClock );
	  };

	}

	*cmdPeriod {
		if( WFSOptions.current.notNil ) {
			ULib.allServers.do({ |srv|
				WFSOptions.current.serverAction.value( srv )
			});
		};
	}

	*doOnServerBoot { |server|
		if( ULib.allServers.includes( server ) ) {
			server.loadDirectory( Platform.userAppSupportDir +/+ "wfs_synthdefs" );
			if( Udef.synthDefDir.notNil ) { server.loadDirectory( Udef.synthDefDir ); };
		};
	}

	*setOutputBusStartOffset { |server, bus|
		var normalServerProgram;
		if( bus != 0 ) {
			if( server.isKindOf( LoadBalancer ) ) {
				if( thisProcess.platform.name == \linux ) { // set jack outputs instead
					normalServerProgram = Server.program;
					server.beforeBootAction = { |srv|
						Server.program = "export SC_JACK_DEFAULT_OUTPUTS=\"%\"; %".format(
							(1..srv.options.numOutputBusChannels).collect({ |item|
								"system:playback_%".format( item + bus)
							}).join(","),
							normalServerProgram
						);
					};
					server.afterBootAction = {
						Server.program = normalServerProgram;
					};
				} {
					server.servers.do({ |srv|
						WFSSpeakerConf.setOutputBusStartOffset( srv, bus );
					});
				};
			} {
				WFSSpeakerConf.setOutputBusStartOffset( server, bus );
			};
		};
	}

	*setServerOptions{ |numOuts=96, numIns = 20, numPrivate = 64|
		Server.default.options
			.maxSynthDefs_(2048)
			.numPrivateAudioBusChannels_(numPrivate)
			.numOutputBusChannels_(numOuts)
			.numInputBusChannels_(numIns)
			.numWireBufs_(2048)
			.memSize_( (2**19).asInteger ) // 256MB
			//.hardwareBufferSize_(512)
			//.blockSize_(128)
			//.sampleRate_( 44100 )
			.maxNodes_( (2**16).asInteger );
		if( Server.default.options.respondsTo( \maxLogins ) ) {
			Server.default.options.maxLogins_(2);
			Server.default.options.bindAddress_("0.0.0.0");
		};

     }

	*initDefaults {
		UChain.makeDefaultFunc = {
			UChain( \bufSoundFile,
				[ \wfsSource,
					[ \point, 5.0.rand2@(5.0 rrand: 10) ] // always behind array
				]
			).useSndFileDur
		};

		UChain.presetManager
		.putRaw( \dynamicPoint, {
			UChain(
				[ \bufSoundFile, [
					\soundFile, BufSndFile.newBasic("@resources/sounds/a11wlk01-44_1.aiff",
						107520, 1, 44100, 0, nil, 1, true)
				] ],
				[ \wfsSource,
					[
						\point, [
							\lag_point, [
								\point, 5.0.rand2@(5.0 rrand: 10),
								\time, 1
							]
						],
						\quality, \better
					] // always behind array
				]
			).useSndFileDur
		})
		.putRaw( \staticPlane, {
			UChain(
				\bufSoundFile,
				[ \wfsSource, [  \point, 5.0.rand2@(5.0 rrand: 10), \type, \plane ] ]
			).useSndFileDur
		})
		.putRaw( \circlePath, {
			UChain(
				[ \bufSoundFile, [
					\soundFile, BufSndFile.newBasic("@resources/sounds/a11wlk01-44_1.aiff",
						107520, 1, 44100, 0, nil, 1, true)
				] ],
				[ \wfsSource, [
					\point, UMap( \circle_trajectory, [ \speed, 0.4 ] ),
					\quality, \better
				] ]
			).useSndFileDur
		})
		.putRaw( \trajectory, {
			UChain(
				\bufSoundFile,
				[ \wfsSource, [
					\point, UMap( 'trajectory', [ \trajectory,
						WFSPathBuffer(
							WFSPath2.generate( 5, 2.4380952380952,
								[ \random, [\seed, 100000.rand, \radius, 10@10] ]
							), 0, 1, true
						)
					] ),
					\quality, \better
				] ]
			).useSndFileDur
		})
		.putRaw( \sinewave, { UChain(
			\sine,
			[ \wfsSource,
				[
					\point, [ \lag_point, [
						\point, 5.0.rand2@(5.0 rrand: 10), \time, 1
					] ],
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
			[ \wfsSource, [
				\point, [ \lag_point, [
					\point, 5.0.rand2@(5.0 rrand: 10), \time, 1
				] ],
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
			[ 'wfsSource', [ 'point', Point(-6, 6) ] ],
			[ 'wfsSource', [ 'type', 'plane', 'point', Point(6, 6), 'u_i_ar_0_bus', 1 ] ],
			[ 'wfsSource', [ 'type', 'plane', 'point', Point(-6, -6), 'u_i_ar_0_bus', 2 ] ]
		)
		);

		PresetManager.all.do({ |pm|
			if( pm.object != WFSOptions ) {
				pm.filePath = Platform.userConfigDir +/+ "default" ++ "." ++ pm.id ++ ".presets";
				pm.readAdd( silent: true );
			};
		});
	}

	*initGUI {

		if(thisProcess.platform.class.asSymbol === 'OSXPlatform' && {
			thisProcess.platform.ideName.asSymbol === \scapp
		}) {
			UMenuBar();
			SCMenuItem.new(UMenuBar.viewMenu, "WFS Position tracker").action_({
				WFSPositionTrackerGUI.newOrCurrent;
				WFSPositionTracker.start;
			});
		} {
			if( UMenuBarIDE.isEmpty ) {

				UMenuBarIDE("WFSCollider");

				UMenuBarIDE.add("WFS", \separator, "View" );

				UMenuBarIDE.add("WFSCollider Servers", {
					ULib.window !? _.front ?? { ULib.serversWindow( "WFSCollider Servers" ) };
				}, "View");

				UMenuBarIDE.add("WFS Position tracker", {
					WFSPositionTrackerGUI.newOrCurrent;
					WFSPositionTracker.start;
				}, "View");

				UMenuBarIDE.preferencesFunc = { WFSOptionsGUI.newOrCurrent; };

				UMenuBarIDE.add( "Servers", \separator, "WFSCollider" );

				UMenuBarIDE.add( "Restart Servers", { WFSLib.startup( WFSOptions.current ); }, "WFSCollider" );

				UMenuBarIDE.add( "Close Servers", { ULib.closeServers; }, "WFSCollider" );

				UMenuBarIDE.add( "Updates", \separator, "WFSCollider" );

				UMenuBarIDE.add( "Check for updates...", { WFSLib.checkForUpdatesGUI }, "WFSCollider" );

				if( UMenuBarIDE.mode == \mainmenu ) { MainMenu.prUpdate(); };
			}
		};

		UGlobalEQ.gui;
	}

	*setGUISkin { |mode = \light, refresh = true|
		Font.default = Font( Font.defaultSansFace, 13 );
		if( UChainGUI.skin != UChainGUI.skins[ mode ] ) {
			UChainGUI.skin = UChainGUI.skins[ mode ] ? UChainGUI.skins[ \light ];
			QtGUI.palette = UChainGUI.skin.qPalette ?? { QPalette.light };
			if( refresh ) {
				UScoreEditorGUI.all.copy.do({ |editor|
					var score;
					score = editor.score;
					editor.askForSave_( false );
					editor.close;
					score.gui;
				});

				UChainGUI.all.copy.do(_.rebuild);

				if( UGlobalEQ.view.view.isClosed.not ) {
					UGlobalEQ.view.view.findWindow.close;
					UGlobalEQ.gui;
				};

				if( ULib.eWindow.notNil && { ULib.eWindow.isClosed.not } ) {
					ULib.envirWindow;
				};

				ULib.serversWindow( "WFSCollider Servers" );

				if( WFSPositionTrackerGUI.current.notNil ) {
					WFSPositionTrackerGUI.current.parent.close;
					WFSPositionTrackerGUI();
				};

				if( WFSOptionsGUI.current.notNil ) {
					WFSOptionsGUI.current.view.findWindow.close;
					WFSOptionsGUI();
				};
			}
		}
	}


	*getCurrentPrefsPath { |action|
		var paths;
		paths = [
			File.getcwd,
			"/Library/Application Support/WFSCollider",
			"~/Library/Application Support/WFSCollider".standardizePath
		].collect(_ +/+ "preferences.scd");

		paths = paths ++ [
			Platform.userConfigDir +/+ "wfscollider_prefs.scd";
		];

		paths.do({ |path|
			if( File.exists( path ) ) {
				action.value( path );
				^path;
			};
		});

		^nil;
	}

	*checkForUpdates { |updatesFoundAction, noUpdatesAction, noConnectionAction|
		var updates;
		updates = [ "Unit-Lib", "WFSCollider-Class-Library", "wslib" ].collect({ |name|
			var status, index, quark;
			quark = Quark(name);
			if( quark.git.branch == "HEAD" ) {
				[ quark, "(no branch)" ]
			} {
				status = quark.git.git( [ "fetch && git status -s -b" ] );
				if( status.size == 0 ) {
					"WFSLib.checkForUpdates; no internet connection".postln;
					^noConnectionAction.value;
				};
				status = status.split( $\n ).first;
				index = status.find("[behind");
				if( index.notNil ) {
					[ quark, status[index..] ]
				};
			}
		}).select(_.notNil);
		if( updates.size > 0 ) {
			updatesFoundAction.value( updates );
			"WFSCollider updates available:".postln;
			updates.do({ |update|
				"\t% %\n".postf( update[0].name, update[1] );
			});
		} {
			noUpdatesAction.value;
			"WFSCollider is up-to-date".postln;
		};
	}

	*updateQuarks { |which, action, noConnectionAction| // run checkForUpdates first
		which = which ? [ "Unit-Lib", "WFSCollider-Class-Library", "wslib" ];
		which.do({ |quark|
			var res;
			if( quark.isString ) { quark = Quark( quark ) };
			if( quark.git.branch == "HEAD" ) {
				"changing branch of % to 'master'\n".postf( quark.name );
				quark.git.git( [ "checkout", "master" ] );
			};
			"updating %\n".postf( quark );
			res = quark.git.git( ["pull", "origin", "master"] );
			if( res.size == 0 ) {
				"WFSLib.updateQuarks; no internet connection".postln;
				^noConnectionAction.value;
			};
			res.postln;
		});
		action.value;
	}

	*checkForUpdatesGUI {
		var cantCheck, bgColor;
		bgColor = UChainGUI.skin[ 'SCAlert' ] !? _.background ?? { Color.white };

		cantCheck = {
			SCAlert("WFSCollider can't check for updates...\n"
				"Most probably there is no internet\n"
				"connection available", ["OK"], [nil],
				Color.green, bgColor, iconName: 'warning'
			)
		};
		this.checkForUpdates( { |upd|
			SCAlert("Updates found for:\n%".format(
				upd.collect({ |item| "  % %".format( item[0].name, item[1] ) }).join("\n") ),
			[ "cancel", "update" ],
			[
				nil,
				{  this.updateQuarks( upd.flop[0], {
					SCAlert( "finished updating, recompile to install\n"
						"WARNING: unsaved changes will be\n"
						"lost at recompile",
						[ "later", "recompile" ],
						[ nil, { thisProcess.recompile }],
						background: bgColor
				)}, cantCheck )
			} ], Color.green, bgColor, 'roundArrow', true );
		}, {
			SCAlert("WFSCollider is up-to-date", ["OK"], [nil],
				Color.green, bgColor, 'clock'
			)
		}, cantCheck );
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

		if( WFSSpeakerConf.default.notNil ) {
			stream << "//speaker configuration:\n";
			stream <<< WFSSpeakerConf.default << ".makeDefault;\n\n";
		};

		WFSOptions.usePresetsForCS = true;

		stream << "//options:\n";
		stream <<< WFSOptions.current << ";";

		WFSOptions.usePresetsForCS = false;

		if( WFSArrayPan.useFocusFades != true ) {
			stream << "\n\nWFSArrayPan.useFocusFades = " << WFSArrayPan.useFocusFades << ";";
		};
		if( WFSArrayPan.efficientFocusFades != false ) {
			stream << "\n\nWFSArrayPan.efficientFocusFades = " << WFSArrayPan.efficientFocusFades << ";";
		};
		if( WFSArrayPan.tapering != 0 ) {
			stream << "\n\nWFSArrayPan.tapering = " <<< WFSArrayPan.tapering << ";";
		};

		^stream.collection;
	}

	*writePrefs { |path|
		var file;
		path = path ? this.getCurrentPrefsPath ?
		    (Platform.userConfigDir +/+ "wfscollider_prefs.scd");
		path.dirname.makeDir;
		"writing preferences file:\n%\n".postf( path );
		file = File( path, "w" );
		file.write( this.formatPrefs );
		file.close;
	}

	*deletePrefs { |path|
		var file;
		path = path ? this.getCurrentPrefsPath ?
			(Platform.userConfigDir +/+ "wfscollider_prefs.scd");
		"rm %".format( path.asString.escapeChar( $ ) ).unixCmd;
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