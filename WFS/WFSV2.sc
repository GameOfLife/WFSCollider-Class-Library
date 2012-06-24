/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2011 Miguel Negrao, Wouter Snoei.

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

    classvar <>debugSMPTE;

    classvar <>syncWrap = 16777216; // == 2**24 == max resolution 32 bits float

    classvar <>previewMode;

    *initClass { debugSMPTE = SMPTE(0, 1000); }

    *debug { |string ... argsArray |
        if( debugMode )
            { (string.asString ++ "\n").postf( *argsArray ); };
        }

    *secsToTimeCode { |secs = 0|
        ^debugSMPTE.initSeconds( secs ).toString;
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
    
    *startupCustom { |config|
		var file, speakers,ip,name, wfsConf;

		Udef.userDefsFolder = File.getcwd +/+ "UnitDefs";		   
		Udef.defsFolders = Udef.defsFolders.add( 
			WFSArrayPan.filenameSymbol.asString.dirname +/+ "UnitDefs"
		);		
		
		WFSSpeakerConf.rect( *config[\speakConf][[0,1,3,2]] * [1,1,0.5,0.5] ).makeDefault;
			
		if(config[\hostname].notNil){
			"starting server mode".postln;
			WFS.startupServer;
		};
			
		if(config[\ips].notNil){
			"starting client mode".postln;
			WFS.startupClient(
				config[\ips],
				config[\startPorts] ?? { 58000 ! 2 },
				config[\scsynthsPerSystem] ? 8,
				config[\hostnames],
				config[\soundCard] ? "MOTU 828mk2",
				config[\numSpeakers] ? 96
			);
		};			
		   
    }
		
	*startup {
		var file, speakers,ip,name, dict, wfsConf;

		Udef.userDefsFolder = File.getcwd +/+ "UnitDefs";		   
		Udef.defsFolders = Udef.defsFolders.add( 
			WFSArrayPan.filenameSymbol.asString.dirname +/+ "UnitDefs"
		);
		
		UnitRack.defsFolders = UnitRack.defsFolders.add( 
			WFSArrayPan.filenameSymbol.asString.dirname +/+ "UnitRacks"
		);
		U.addUneditableCategory(\wfs_panner);
		
		WFSSpeakerConf.rect( 48, 48, 5, 5 ).makeDefault;
		
		GlobalPathDict.put( \wfs, "/WFSSoundFiles" );
		GlobalPathDict.put( \resources, String.scDir );
		
		UChain.makeDefaultFunc = {
			UChain( \bufSoundFile, 
				[ \wfsStaticPoint, 
					[ \point, (5@0).rotate(2pi.rand) ]
				]
			).useSndFileDur
		};
		
		if( File.exists( "/Library/Application Support/WFSCollider/WFSCollider_configuration.txt" ) ) {
			file = File("/Library/Application Support/WFSCollider/WFSCollider_configuration.txt","r");
			dict = file.readAllString.interpret;
			file.close;
			WFSSpeakerConf.rect( *dict[\speakConf][[0,1,3,2]] * [1,1,0.5,0.5] ).makeDefault;
			
			if(dict[\hostname].notNil){
				"starting server mode".postln;
				WFS.startupServer;
			};
			
			if(dict[\ips].notNil){
				"starting client mode".postln;
				WFS.startupClient(
					dict[\ips], 
					dict[\startPorts] ?? { 58000 ! 2 }, 
					dict[\scsynthsPerSystem] ? 8, 
					dict[\hostnames], 
					dict[\soundCard] ? "MOTU 828mk2" 
				);
			};
			
		} {
			"starting offline".postln;
			WFS.startupOffline;
		};
		if(thisProcess.platform.class.asSymbol == 'OSXPlatform') {
		    UMenuBar();
		    SCMenuItem.new(UMenuBar.viewMenu, "WFSPositionTracker").action_({
				WFSPositionTrackerGUI.newOrCurrent;
			});
		};

	}
		
    *startupOffline {
        var server, defs;

        this.setServerOptions(20);

        server = WFSServers.single.makeDefault;
        
        this.previewMode = \headphone;

        WFSSpeakerConf
            .numSystems_(1)
            .addServer( server.m, 0 );

        defs = Udef.loadAllFromDefaultDirectory.collect(_.synthDef).flat.select(_.notNil)
        ++WFSPrePanSynthDefs.generateAll.flat++WFSPreviewSynthDefs.generateAll;

        UnitRack.loadAllFromDefaultDirectory;

        server.boot;
        server.makeWindow;
        server.m.waitForBoot({

            defs.do({|def|
                def.load( server.m );
            });



            SyncCenter.loadMasterDefs;

            // WFSLevelBus.makeWindow;

            "\n\tWelcome to the WFS Offline System V2".postln
        });

        Server.default = WFSServers.default.m;
        ULib.servers = [ Server.default ];
        WFSPathBuffer.writeServers = [ Server.default ];

        UGlobalGain.gui;
        UGlobalEQ.gui;

        ^server
    }

    *startupClient { |ips, startPort, serversPerSystem = 8, hostnames,
            soundCard = "MOTU 828mk2", numSpeakers = 96|
        var server;
        this.setServerOptions(numSpeakers);

        if(thisProcess.platform.class == OSXPlatform) {
            Server.default.options.device_( soundCard );
        };
        server = WFSServers( ips, startPort, serversPerSystem ).makeDefault;
        server.hostNames_( *hostnames );

        server.makeWindow;
        
         this.previewMode = nil;

        WFSSpeakerConf.numSystems_( ips.size );

        server.multiServers.do({ |ms, i|
            ms.servers.do({ |server|
                WFSSpeakerConf.addServer( server, i );
            });
        });

        SyncCenter.writeDefs;

        server.m.waitForBoot({
            var defs;
            SyncCenter.loadMasterDefs;

             defs = Udef.loadAllFromDefaultDirectory.collect(_.synthDef).flat.select(_.notNil);

            defs.do({|def|
                    def.load( server.m );
              });

            UnitRack.loadAllFromDefaultDirectory;
            /*
            server.multiServers.do({ |ms|
                ms.servers.do({ |server|
                    defs.do({|def|
                        def.send( server )
                    })
                })
            });
            */

            // WFSLevelBus.makeWindow;
            "\n\tWelcome to the WFS System".postln;
        });

        Server.default = WFSServers.default.m;

        ULib.servers = [ Server.default ] ++
            WFSServers.default.multiServers.collect({ |ms|
                LoadBalancer( *ms.servers )
            });
        WFSPathBuffer.writeServers = ULib.servers.collect{ |s| s.asTarget.server };

        UGlobalGain.gui;
        UGlobalEQ.gui;

        ^server
    }

    *startupServer { |hostName, startPort = 58000, serversPerSystem = 8, soundCard = "JackRouter"|
        var server, serverCounter = 0;

        if( Buffer.respondsTo( \readChannel ).not )
            { scVersion = \old };

        this.setServerOptions;

        Server.default.options.device_( soundCard );
        server = WFSServers.client(nil, startPort, serversPerSystem).makeDefault;
        server.hostNames_( hostName );
        server.boot;
        server.makeWindow;

        Routine({
            var allTypes, defs;
            while({ server.multiServers[0].servers
                    .collect( _.serverRunning ).every( _ == true ).not; },
                { 0.2.wait; });
            //allTypes = WFSSynthDef.allTypes( server.wfsConfigurations[0] );
            //allTypes.do({ |def| def.def.writeDefFile });
            // server.writeServerSyncSynthDefs;

            defs = Udef.loadAllFromDefaultDirectory.collect(_.synthDef).flat.select(_.notNil);

            defs.do({|def| def.writeDefFile; });
            SyncCenter.writeDefs;
            server.multiServers[0].servers.do({ |server|
                server.loadDirectory( SynthDef.synthDefDir );
                });

            ("System ready; playing lifesign for "++hostName).postln;
            (hostName ++ ", server ready").speak

        }).play( AppClock );
        ^server // returns an instance of WFSServers for assignment
        // best to be assigned to var 'm' in the intepreter
    }
	
}