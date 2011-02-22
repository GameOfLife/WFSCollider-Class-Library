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

WFSServers {

	// 3 possible types:
	//    - master: a local master server and a number of remote client multiservers
	//    - client: no master, only one set of client servers 
	//    - single: only a master server; meant for use on a single system
	
	classvar <>default;
	classvar <>pulsesNodeID = 5;
	classvar <>syncDelayBusID = 0;
	classvar <>maxStartsPerCycle = 25; // high values give errors with longer scores
	classvar  <>wrapTime = 20.0;
	classvar <>pulsesOutputBus = 14;
	
	var <ips, <startPort, <serversPerSystem;
	var <multiServers;
	var <masterServer;
	var <basicName = "wfs";
	var <window;
	var serverLabels;
	var <>pointer = 0;
	var <>wfsConfigurations;
	var <activityIndex;
	var <activityFromCPU = false;
	var <syncDelays;
	var <pulsesRunning = false;
	var <counterRunning = false;
	var pulsesRunningView;
	var delayViews;
	var <>singleWFSConfiguration;
	var <>debugWatchers;
	
	var <>activityDict; // added 16/04/2009 - split activity spreading
	
	//*initClass { default = WFSServers( "127.0.0.1" ); }
	
	*new { |ips, startPort = 58000, serversPerSystem = 8|
		^super.newCopyArgs( ips, startPort, serversPerSystem ).init;
	}
	
	*master { |ips, startPort = 58000, serversPerSystem = 8| 
		^this.new( ips, startPort, serversPerSystem )
		}
	
	*client { | ip, startPort = 58000, serversPerSystem = 8|
		ip = ip ? "127.0.0.1";
		^super.newCopyArgs( [ip], [startPort], serversPerSystem ).init( false );
		}
		
	*single { ^super.newCopyArgs.init; }
		
	init { |addMaster = true|
		[ips,startPort,serversPerSystem].postln;
		multiServers = ips.collect({ |ip, i|
			MultiServer.row( 
				serversPerSystem, 
				basicName ++ (i+1) ++ "_", 
				NetAddr( ip, startPort[i] ), 
				Server.default.options , 
				synthDefDir: "/Applications/WFSCollider.app/Contents/Resources/synthdefs/");
			}) ? [];
			
		syncDelays = { { 0 }!serversPerSystem }!ips.size;
		delayViews = { { nil }!serversPerSystem }!ips.size;
		
		CmdPeriod.add( this );
			
		if( addMaster ) { 
			masterServer = Server( 
				"wfs_master", 
				NetAddr("127.0.0.1", startPort[0] - 1 ),
				Server.default.options.copy;
				);
			};
		
		activityIndex = 0!serversPerSystem;
		activityDict = IdentityDictionary[];
		multiServers.do({ |ms| ms.servers.do({ |srv| activityDict[ srv ] = 0 }) });
		
		if( this.isMaster ){
			SyncCenter.addAll(multiServers.collect{ |msv| msv.servers }.flat);
		}	
	}
		
	m { ^masterServer; }
	
	freeAllBuffers {
		multiServers.do({ |ms, i|
			ms.servers.do({ |srv, ii| 
				srv.bufferArray.select( _.notNil ).do({
					|buffer| 
						"wfs%_%: freed buffer % (% samples)\n"
							.postf( i, ii, buffer.bufnum, buffer.numFrames );
						buffer.free; });
					});
				});
						
		}
	
	boot { if( masterServer.notNil )
			{ masterServer.boot; };
		multiServers.do( _.boot(10) );
		}
	
	cmdPeriod { pulsesRunning = false; counterRunning = false; Server.freeAllRemote( false ); }
	
	isMaster { ^( masterServer.notNil && { multiServers.size != 0 }) }
	isClient { ^( masterServer.isNil && { multiServers.size == 1 }) }
	isSingle { ^( masterServer.notNil && { multiServers.size == 0 }) }
	
	hasMasterServer { ^masterServer.notNil }
	
	makeWindow {
		var comp, widgets = List.new;
		
		if( window.notNil && { window.isClosed.not }) { window.front; ^this };
		
		window = SCWindow("WFSServers", Rect(10, 10, 440, 8 +
			( (ips.size * serversPerSystem)  * (22)) + (ips.size * 16 ) ), false).front;
		
		window.onClose_({ widgets.do(_.remove) });
		
		window.view.decorator = FlowLayout(window.view.bounds);

		
		if( this.hasMasterServer )
			{ 
			//delayViews = syncDelays.copy;
			window.bounds = window.bounds + Rect( 0, 0, 0, 45 + 20 );
			
			SCButton( window, Rect( 0, 0, 16, 16 ) )
				.states_([["K", Color.black, Color.clear]])
				.font_( Font( "Monaco", 9 ) )
				.action_( { Server.killAll;
					if(this.isMaster) {
						"ssh gameoflife@192.168.2.11 \"killAll scsynth\"".systemCmd;
						"ssh gameoflife@192.168.2.12 \"killAll scsynth\"".systemCmd;
					}
				} );
			
			SCButton(window, Rect(0,0, 16, 16))
				.states_([["F", Color.black, Color.clear]])
				.font_( Font( "Monaco", 9 ) )
				.action_( { Server.freeAllRemote; } );
				
			SCButton(window, Rect(0,0, 16, 16))
				.states_([["R", Color.black, Color.clear]])
				.font_( Font( "Monaco", 9 ) )
				.action_( { ServerRecordWindow( masterServer, 999 ); } );
			
			SCButton(window, Rect(0,0, 16, 16))
				.states_([["?", Color.black, Color.clear]])
				.font_( Font( "Monaco", 9 ) )
				.action_( { WFS.openHelpFile } );
			
			SCStaticText( window, Rect( 0, 0, 140, 15 ) )
				.string_( "master (" ++ ( NetAddr.myIP ? "127.0.0.1" ) ++ ")" )
				.font_( Font( "Monaco", 9 ) );

			pulsesRunningView = SCStaticText( window, Rect( 0, 0, 100, 16 ) )
				.stringColor_( Color.red )
				.font_( Font( "Monaco", 9 ) )
				.string_( "" );
				
			window.view.decorator.nextLine;
			masterServer.makeView( window ); 
			if( this.isMaster ) {
			
				window.bounds = window.bounds + Rect( 0, 0, 0, 45 + 20 + 20 );
				
				SCButton( window, Rect( 0, 0, 110, 16 ) )
					.states_( [["sync"]] )
					.font_( Font( "Monaco", 9 ) )
					.action_( {
						SyncCenter.remoteSync;	
					} );
				
				widgets.add(SyncCenterStatusWidget(window,17));		
						
				SCButton( window, Rect( 0, 0, 90, 16 ) )
					.states_( [["open hosts"]] )
					.font_( Font( "Monaco", 9 ) )
					.action_( { this.openHosts; } ); 
					
				SCButton( window, Rect( 0, 0, 110, 16 ) )
					.states_( [["shut down hosts"]] )
					.font_( Font( "Monaco", 9 ) )
					.action_( { SCAlert( "Do you really want to shut down\nboth host servers?",
							 ["cancel", "unmount only", "restart SC", "Shut Down"], 
							 [{}, 
							 { // "~/Unmount servers.app".openInFinder 
							  "umount /WFSSoundFiles".unixCmd;
								 },
							 { //"~/Stop_SC_Servers.command".openInFinder;
			"ssh gameoflife@192.168.2.11 \"open \\\"Stop_SC.app\\\"\"".systemCmd;
			"ssh gameoflife@192.168.2.12 \"open \\\"Stop_SC.app\\\"\"".systemCmd; 
							  	SCAlert( "Do you want to startup SC again?",
							  		[ "no", "yes" ],
							  		[ {}, { 
							  		
							  	//"~/ResartSC_Servers.command".openInFinder;
				"ssh gameoflife@192.168.2.11 \"open \\\"/Applications/SuperCollider/SuperCollider.app\\\"\"".systemCmd;
				"ssh gameoflife@192.168.2.12 \"open \\\"/Applications/SuperCollider/SuperCollider.app\\\"\"".systemCmd;
							  	 }] );
							  },
							 {
							  //"~/Unmount servers.app".openInFinder;
							  "umount /WFSSoundFiles".unixCmd;
							  
							{	var win, views;
								
								win = SCWindow( "", 
									Rect( *(Rect(0,0,800,250)
										.centerIn( SCWindow.screenBounds).asArray 
											++ [800,250]) ) ).front;
								win.view.background_( Color.white
									.blend( Color.red(0.6), 0.25 ) );
								
								win.drawHook = { |win| 
									Color.red.set; 
									Pen.width_( 30 );
									Pen.strokeRect( win.view.bounds );   
									};
								
								//win.decorate;
									
								views = ();
								
								SCStaticText( win, win.view.bounds.copy.height_( 150 ) )
									.string_( "WARNING:\nAre the AMPLIFIERS switched OFF?" )
									.font_( Font( "Helvetica-Bold", 40 ) )
									.align_( \center );
								
								SCButton( win, Rect(80,150,180,50) )
									.states_( [[ "cancel", Color.black, Color.white ]] )
									.action_({ |bt| win.close; })
									.font_( Font( "Helvetica-Bold", 30 ) );
								
								SCButton( win, Rect(320,150,400,50) )
									.states_( [[ "yes, shut down now", 
												Color.black, Color.white ]] )
									.font_( Font( "Helvetica-Bold", 30 ) )
									.action_({ |bt| win.close;
									 // "~/Shutdown_Servers.command".openInFinder;
									 "shutting down hosts..".postln;
									"open \"Unmount servers.app\"".systemCmd;
					"ssh gameoflife@192.168.2.11 \"open \\\"shutdown.app\\\"\"".unixCmd;
					"ssh gameoflife@192.168.2.12 \"open \\\"shutdown.app\\\"\"".unixCmd;
									
									 }).focus;
									}.value;


							  }] )
						.iconName_( \power )
						.color_( Color.blue(0.25) ); } ); 
				
				window.view.decorator.nextLine;
				};
			};
			
		serverLabels = nil!multiServers.size;
		multiServers.do({ |multiServer, i| 
		
			SCButton( window, Rect( 0, 0, 12, 12 ) )
				.states_([["k", Color.red, Color.red.alpha_(0.1)]])
				.font_( Font( "Monaco", 9 ) )
				.action_( {
					// kill synths and press cmd-k on remote
					"killing scsynths on server % and recompiling, please wait ± 5s plus another few for lifesign\n"
						.postf( ips[i].asString );
					("killall -9 scsynth; sleep 2; killall -9 scsynth; sleep 2;" +  // kills twice with wait in between
					 "k".asKeyStrokeCmd( "command" ) )
						.sshCmd( "gameoflife", ips[i].asString );
			} );
			
			SCButton( window, Rect( 0, 0, 12, 12 ) )
				.states_([["x", Color.red, Color.red.alpha_(0.1)]])
				.font_( Font( "Monaco", 9 ) )
				.action_( {
					// kill synths and restart sc on remote
					"killing scsynths on server % and rebooting sc, please wait ± 10s plus another few for lifesign\n"
							.postf( ips[i].asString );
					("killall -9 scsynth; sleep 2; killall -9 scsynth; sleep 2;" +
				 	 "open 'Stop_SC.app'; sleep 3;" +
				 	 "open '/Applications/SuperCollider/SuperCollider.app'")
						.sshCmd( "gameoflife", ips[i].asString );
			} );
					 
			serverLabels[i] = SCStaticText( window, Rect( 0, 0, 300, 12 ) )
				.string_("multi" ++ (i+1) ++ " (" ++ 
					if( ips[i] == "127.0.0.1",
						{ NetAddr.myIP ? "127.0.0.1" }, { ips[i] } )
					++ "/" ++ multiServer.hostName ++ ")" )
				.font_( Font( "Monaco", 9 ) );
			window.view.decorator.nextLine;
			
			multiServer.servers.do({ |server, ii| 
				if( this.isMaster ) {
					widgets.add(SyncCenterServerWidget(window,100@17,server))
				};
				server.makeView( window ); 
				});
			});
			
		if( this.isSingle ) { window.bounds = window.bounds + Rect(0,240,0,45+20); };
		
		//window.view.decorator.nextLine;
		}
		
	makeDefault { default = this }
	
	allServers { ^masterServer.asCollection ++ multiServers.collect({ |ms| ms.servers }).flat }
	
	at { |index| 
		 if( this.isSingle ) 
		 	{ ^[masterServer] } // if single always master server
			{ ^multiServers.collect({ |ms,i| 
				ms.servers.wrapAt( index.asCollection.wrapAt(i) )  // ws 16/04/09
				}); } 
		}
		
	current { if( this.isSingle ) 
		{ ^[masterServer] } 
		{ ^this.at( pointer ); } 
		}
	
	updateActivity { if( activityFromCPU && { this.isSingle.not } )
		{ activityIndex = multiServers[0].servers.collect( _.avgCPU );  };
		}
	
	addActivity { |value = 0, index| 
		activityIndex[ index ? pointer ] = activityIndex[ index ] + value; }
	
	removeActivity { |value = 0, index| 
		activityIndex[ index ? pointer ] = activityIndex[ index ] - value; }
		
	setActivity { |value = 0, index| activityIndex[ index ? pointer ] = value; }
	resetActivity { activityIndex = 0!serversPerSystem; }
	
	leastActiveIndex { ^activityIndex.normalize.detectIndex( _ == 0 ); }
	
	next {
		if( this.isSingle.not )
			{this.updateActivity; pointer = this.leastActiveIndex; };
		^this.current; 
		}
		
	currentSyncDelay { ^syncDelays.flop[ pointer ] ? 0 }
	currentSyncDelayS { ^this.currentSyncDelay / 44100 }
	
	nextIndex { |addActivity = 0|
		if( this.isSingle.not )
			{this.updateActivity;  pointer = this.leastActiveIndex; };
		//pointer = this.leastActiveIndex;
		this.addActivity( addActivity, pointer );
		^pointer 
		}
	
	nextArray { |addActivity = 0|
		if( this.isSingle.not )
			{this.updateActivity; pointer = this.leastActiveIndex; 
				this.addActivity( addActivity, pointer );
				^[pointer, this.current, this.currentSyncDelayS ];
			}
			{ ^[0, this.masterServer, 0 ] }
		
		}
		
	hostNames { ^multiServers.collect( _.hostName ); }
	hostNames_ { |... names| 
		names.do({ |name, i| multiServers[ i ].hostName = name.asString; });
		}
		
	openHosts { |login = "gameoflife", pwd = "192x"|
		multiServers.do({ |ms, i|
			if( ms.hostName.size != 0 )
				{ ms.openHost( login, pwd );  }
				{ "WFSServers-openHosts: MultiServer %: hostname not specified\n".postf( i ) };
			});
		}
		
	loadWFSSynthDefs {
		case { this.isSingle }
			{ WFSSynthDef.allTypes( singleWFSConfiguration )
				.do({ |def| def.load( masterServer ) }); }
			{ this.isClient }
			{
			multiServers.do({ |ms, i|
				var defs;
				defs = WFSSynthDef.allTypes( wfsConfigurations.wrapAt[i] );
				defs.do({ |def| def.load(ms[0]);
						ms.servers[1..].do({ |server| def.send( server ); });
					});
				});
			}
			{ this.isMaster }
			{ "please load synthdefs locally on your server machines".postln };
			 
		}

	startDebugWatcher {
		if(multiServers.notNil){
			debugWatchers = multiServers.collect{ |multiServer|
				multiServer.servers.collect{ |server|
					var debug = DebugNodeWatcher(server);
					debug.start;
					debug;
				}
			}.flat
		}
	}

	stopDebugWatcher {
		debugWatchers.do{ |watcher|
				watcher.stop
		}
	}
	
	}