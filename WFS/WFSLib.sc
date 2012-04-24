WFSLib {
	
	*startup { |wfsOptions|
		var defs, servers, o;
		var bootFunc;
		
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
				).makeDefault;
				WFSPathBuffer.writeServers = WFSServers.default.multiServers.collect(_[0]);
			} {
				WFSServers.single( ).makeDefault;
				WFSPathBuffer.writeServers = [ WFSServers.default.m ];
			};
			
			Server.default = WFSServers.default.m;
			o = wfsOptions.masterOptions;
			WFSServers.pulsesOutputBus = o.toServersBus;
			
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
		
		WFS.setServerOptions( o.numOutputBusChannels );
		Server.default.options.device = o.device;
		Server.default.options.numInputBusChannels = o.numInputBusChannels;
		
		WFS.previewMode = wfsOptions.previewMode;
		
		servers = servers ++ WFSServers.default.multiServers.collect({ |ms|
			LoadBalancer( *ms.servers ) 
		});
		
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
		
		defs = Udef.loadAllFromDefaultDirectory.collect(_.synthDef).flat.select(_.notNil);
		UnitRack.loadAllFromDefaultDirectory;
		
		defs.do({|def| def.writeDefFile; });
		if( SyncCenter.mode == 'sample' ) {
			SyncCenter.writeDefs;
		};
		
		ULib.servers = servers;
		
		if( wfsOptions.showGUI ) {
			
			WFSServers.default.makeWindow;
			
			if(thisProcess.platform.class.asSymbol == 'OSXPlatform') {
			    UMenuBar();
			};
	
			UGlobalGain.gui;
			UGlobalEQ.gui;
		};
		
		WFSServers.default.boot;
		 
	}
	
}