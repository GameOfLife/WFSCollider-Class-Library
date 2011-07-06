/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2010 Wouter Snoei.

    GameOfLife WFSCollider software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife WFSCollider is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife WFSCollider.  If not, see <http://www.gnu.org/licenses/>.
*/

WFS {
	
	classvar <>graphicsMode = \fast;
	classvar <>scVersion = \new;
	classvar <>debugMode = false;
	classvar <>mode = \score; // score or live 
	
	classvar <>debugSMPTE;
	
	classvar <>syncWrap = 16777216; // == 2**24 == max resolution 32 bits float
	
	*initClass { debugSMPTE = SMPTE(0, 1000); }
	
	*debug { |string ... argsArray |
		if( debugMode )
			{ (string.asString ++ "\n").postf( *argsArray ); };
		}
		
	*secsToTimeCode { |secs = 0|
		^debugSMPTE.initSeconds( secs ).toString;
		}
		
	*startup{
		var file, speakers,ip,name, dict, wfsConf;
		
		 WFSConfiguration.default = WFSConfiguration.rect4(  40, 56, 0.165, 8.45, 10.40 );
		 WFSTransport.autoReturnToZero = true;  // performance mode
		 WFSTransport.autoSwitchToNext = false;

		if( File.exists( "/Library/Application Support/WFSCollider/WFSCollider_configuration.txt" ) ) {
			file = File("/Library/Application Support/WFSCollider/WFSCollider_configuration.txt","r");
			dict = file.readAllString.interpret;
			file.close;
			wfsConf = WFSConfiguration.rect4( dict[\speakConf][0],  dict[\speakConf][1], 0.165,  dict[\speakConf][2],  dict[\speakConf][3] );
			if(dict[\hostname].notNil){
				"starting server mode".postln;
				WFSConfiguration.default = wfsConf.partial(dict[\serverNumber],dict[\numberOfServers]);
				WFS.startupServer( dict[\hostname], dict[\startPort] ? 58000, dict[\scsynthsPerSystem] ? 8 , dict[\soundCard] ? "JackRouter" );
			};
			
			if(dict[\ips].notNil){
				"starting client mode".postln;
				WFSConfiguration.default = wfsConf;
				WFS.startupClient( dict[\ips], dict[\startPorts] ?? { 58000 ! 2 }, dict[\scsynthsPerSystem] ? 8, dict[\hostnames], dict[\soundCard] ? "MOTU 828mk2" );
			};
			
		}{
			"starting offline".postln;
			WFS.startupOffline
		};
		WFSMenubar.add;
		Document.initAction = { |doc|
		if( (doc.string(0, 9) ? "").toLower == "<xml:wfs>" )
			{ SCAlert( 
				"This document appears to be a WFS score file. Do you want to open it in the score editor?", 
					[ "no", "yes" ], [ { }, { 
						WFSScore.readWFSFile( doc.path ).edit;
						doc.close;
				 } ]);
			};
		};
	}	
	
	*setServerOptions{ |numOuts=96|
		Server.default.options
			.numAudioBusChannels_(256)
			.numOutputBusChannels_(numOuts)
			.numInputBusChannels_(20)
			.numWireBufs_(2048)
			.memSize_(2**19) // 256MB
			.hardwareBufferSize_(512)
			.blockSize_(128)
			.sampleRate_( 44100 )
			.blockAllocClass_( ContiguousBlockAllocator ); 
		
	}
	
	*startupOffline{
		var server;
		
		if( Buffer.respondsTo( \readChannel ).not ) { 
			scVersion = \old
		};
		
		this.setServerOptions(20);

		server = WFSServers.single.makeDefault;
		server.singleWFSConfiguration = WFSConfiguration.stereoLine(1.3).useSwitch_( false );
		server.boot;
		server.makeWindow;
		server.m.waitForBoot({ 
			server.loadWFSSynthDefs;
			WFSLive.loadSynthDefs( server.m );
			WFSEQ.new; WFSTransport.new; WFSLevelBus.makeWindow;
			
			"\n\tWelcome to the WFS Offline System".postln	
		});
		
		^server
		
	}
	
	*startupClient{ |ips, startPort, serversPerSystem = 8, hostnames, soundCard = "MOTU 828mk2"|
		var server;
		if( Buffer.respondsTo( \readChannel ).not )
			{ scVersion = \old };
		this.setServerOptions;
		Server.default.options.device_( soundCard );
		server = WFSServers( ips, startPort, serversPerSystem ).makeDefault;
		server.hostNames_( *hostnames );
		
		server.wfsConfigurations = 
			[ WFSConfiguration.halfRect2_1, WFSConfiguration.halfRect2_2 ];
				
		// live eq (todo: incorporate in software)
		WFSEQ.action = { |eq, label, val, desc|
			WFSServers.default.allServers
				.do({ |sv| sv.sendMsg( "/n_set", 1, label, val ); });
		};
		// end life eq
		server.makeWindow;	
		server.m.waitForBoot({ 
			"\n\tWelcome to the WFS System".postln;
			server.loadClientSyncSynthDefs; 
		});	
		^server	
	}
	
	*startupServer{ |hostName, startPort = 58000, serversPerSystem = 8, soundCard = "JackRouter"|
		var server, serverCounter = 0;
		
		if( Buffer.respondsTo( \readChannel ).not )
			{ scVersion = \old };
		
		this.setServerOptions;	
		
		Server.default.options.device_( soundCard );
		server = WFSServers.client(nil, startPort, serversPerSystem).makeDefault;
		server.hostNames_( hostName );
		server.wfsConfigurations = [ WFSConfiguration.default ];
		server.boot;
		server.makeWindow;	

		Routine({ 
			var allTypes;
			while({ server.multiServers[0].servers
					.collect( _.serverRunning ).every( _ == true ).not; },
				{ 0.2.wait; });
			allTypes = WFSSynthDef.allTypes( server.wfsConfigurations[0] );
			allTypes.do({ |def| def.def.writeDefFile });
			WFSLive.writeSynthDefs;
			server.writeServerSyncSynthDefs;
			server.multiServers[0].servers.do({ |server|
				server.loadDirectory( SynthDef.synthDefDir );
				});

			("System ready; playing lifesign for "++hostName).postln;
			(hostName ++ ", server ready").speak

		}).play( AppClock );
		^server // returns an instance of WFSServers for assignment
		// best to be assigned to var 'm' in the intepreter
	}

	*startupSingleMachine{ |ips, startPort, serversPerSystem = 8, hostnames, soundCard = "MOTU 828mk2", config|
		var server;
		if( Buffer.respondsTo( \readChannel ).not )
			{ scVersion = \old };
		this.setServerOptions;
		Server.default.options.device_( soundCard );
		server = WFSServers( ips, startPort, serversPerSystem ).makeDefault;
		server.hostNames_( *hostnames );

		server.wfsConfigurations = [ config ];
		server.boot;
		// live eq (todo: incorporate in software)
		WFSEQ.action = { |eq, label, val, desc|
			WFSServers.default.allServers
				.do({ |sv| sv.sendMsg( "/n_set", 1, label, val ); });
		};
		// end life eq
		server.makeWindow;
		server.m.waitForBoot({
			"\n\tWelcome to the WFS System".postln;
		});
		Routine({
			var allTypes;
			while({ server.multiServers[0].servers
					.collect( _.serverRunning ).every( _ == true ).not; },
				{ 0.2.wait; });
			
			WFSLive.writeSynthDefs;
			server.multiServers[0].servers.do({ |server|
				server.loadDirectory( SynthDef.synthDefDir );
				});
		}).play( AppClock );
		^server
	}

	*makeArchive { |version = "0.5b"| 
		WFS.filenameSymbol.asString.dirname.dirname
			.targz( "~/Desktop/WFS_Classes_v" ++ version );
		}

 }

