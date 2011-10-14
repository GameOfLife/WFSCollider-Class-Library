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

// Sync System (based on pulse via digital connection to clients)


+ WFSEvent {

	typeActivity { ^wfsSynth.typeActivity;
		/*
		var type;
		type = wfsSynth.intType;
		^( 'linear': 12, 'static': 3.5, 'cubic': 18, 'index': 0.5, 'plane': 3.5 )[ type ] ? 0
		*/
		}
	}
	
+ WFSScore {

	*stop {
			WFSSynth.clock.clear; 
			SystemClock.clear;
			WFSSynth.freeAllBuffers;
			WFSSynth.freeAllSynths;
			WFSSynth.resetLoadedSynths;
			WFSServers.default.resetActivity;
			WFSServers.default.resetDictActivity;
		}
		
	stop { this.class.stop }
		
	}
	
+ WFSSynth {

	typeActivity { var type;
		type = this.intType;
		/*
		// G5
		^( 'linear': 12, 'static': 3.5, 'cubic': 18, 'index': 0.5, 'plane': 3.5,
			'switch': 12 )[ type ] ? 0
		*/
		
		// intel
		/*
		^( 'linear': 12.5, 'static': 6, 'cubic': 19, 'index': 0.5, 'plane': 6,
			'switch': 12.5 )[ type ] ? 0
		*/
		
		// feb 2009 (AES)	
		^( 'linear': 6.6, 'static': 3.8, 'cubic': 10.4, 'index': 0.4, 'plane': 3.2,
			'switch': 6.6 )[ type ] ? 0
		}
	
	*resetLoadedSynths { loadedSynths = []; }
	
	free { synth.asCollection.do( _.dispose ); isRunning = false; }
	
	release { synth.asCollection.do( _.release ); isRunning = false; }
	
	freeBuffers {
		if( buffersLoaded.not ) { 
			"WFSSynth-freeBuffers: buffers are probably not loaded".postln; };
		
		if( wfsPath.class == WFSPath ) { wfsPath.freeBuffers; };
		
		delayBuffer.asCollection.do( _.free );
	
		if( wfsDefName.wfsAudioType === 'disk')
			{ sfBuffer.asCollection.do( _.close ); };
		
		if( sfBuffer.notNil ) { sfBuffer.asCollection.do( _.free ) };
			
		buffersLoaded = false;
			
	}

	
	loadBuffers { |servers|
		if( buffersLoaded.not ) {	
		  	if(wfsPath.class == WFSPath ) {
		  		wfsPath.loadBuffers2( servers );
		  	};
				
			if( [ 'linear', 'cubic', 'switch','static','plane']
					.includes( wfsDefName.wfsIntType ) ) {
				delayBuffer = WFSPan2D.makeBuffer( servers ); 
			};	
					
			servers.asCollection.do({ |oneServer|
									
				case { wfsDefName.wfsAudioType === 'buf' }
					{ sfBuffer = sfBuffer.asCollection.add(
						if( WFS.scVersion === \new,
						{	Buffer.readChannel( 
								oneServer, filePath, startFrame ? 0,
								numFrames: this.samplesPlayed, //only load needed
								channels: [0] // only the first channel
								)  },
						{	Buffer.read( 
								oneServer, filePath, startFrame ? 0,
								numFrames: (-1)
								)   })
						);
						if( WFSPan2D.silent )
							{ 
						if( sfBuffer.size > 1 )
								{ 
						WFS.debug( "'%' read to buffers %\n",
								filePath.basename, 
									sfBuffer.collect({ |buf|
										[oneServer.name, buf.bufnum]})
								//sfBuffer.collect(_.bufnum)
								 )
								};
							 };
					}
					{ wfsDefName.wfsAudioType === 'disk'}
					{ sfBuffer = sfBuffer.asCollection.add(
							Buffer.cueSoundFile( oneServer, filePath, startFrame ? 0,
							numChannels:1,
							bufferSize: 32768 * 2
					) );
					if( WFSPan2D.silent ) { 
						if( sfBuffer.size > 1 )
							{  WFS.debug( "'%' cued in buffers %\n",
							filePath.basename, 
								sfBuffer.collect({ |buf|
									[oneServer.name, buf.bufnum]})) 
							}; };
					 };
				});
			buffersLoaded = true;
			WFS.debug("buffers loaded");
			
		  } { "WFSSynth-loadBuffers: Buffers are probably already loaded".postln; };
		
	}
				
	load { |servers, syncCenter |
		var defName, time, delta, chain, bundles;
			
	  	//this.loadBuffers( servers );
	  	chain = this.getUChain;
        synth = chain.prepare(servers,false);
	  	Routine({
		  	// wait with loading synth
		  	// so all buffers have been allocated already
		  	(WFSEvent.wait-SyncCenter.latency).wait;
		  	//testing while wfs panners are not ready
		    //chain.start(servers)

            bundles = chain.makeBundle(servers);
            servers.do({ |server, i|
                server.listSendSyncedBundle(SyncCenter.latency, bundles[i], syncCenter)
            });

	  		/*if( useFocused.not ) {
	  			defName = ("WFS_" ++ this.intType ++ "Out_" ++ this.audioType).asSymbol;
	  		} {
	  			defName = wfsDefName
	  		};
	  	 	
	  	 	synth = WFSSynth.generateSynth( wfsDefName, wfsPath,
					servers, delayBuffer, sfBuffer, 
					pbRate, level, loop, dur, input, args, fadeTimes,
					wfsPathStartIndex, SyncCenter.latency, syncCenter );

			//automatically done by UChain - NOT NEEDED
			synth.do{ |syn|
				syn.register(true);

				syn.freeAction_({
					this.freeBuffers;
					isRunning = false;
					loadedSynths.remove( this );
					WFSServers.default.removeDictActivity( this.typeActivity, servers );
					WFS.debug("loaded synths: % (removed one)", loadedSynths.size);
				})
			}
			*/

		}).play(SystemClock);
		
				
		if(WFS.debugMode){
			("STARTING SYNTH "++thisThread.seconds++" wfsDefName:"++wfsDefName).postln;												time = thisThread.seconds;

		};
									
		loaded = true;
		loadedSynths = loadedSynths.asCollection.add( this );
		WFS.debug("loaded synths: % ( added one )",loadedSynths.size);
			
			
	}
			
	prepareForPlayback { 
			if( wfsPath.class == WFSPath )
				{ wfsPath.resetBuffers };
			^this.resetFlags.clearVars;
			// really copy..
			/* ^this.class.new(  wfsDefName, wfsPath.copyNew, server, filePath, dur, 
				level, pbRate, loop, input, args, fadeTimes, startFrame );
			*/
	}		
		
	playNow{  |wfsServers, startTime = 0, syncCenter |
		if(WFSServers.default.isSingle){
			this.playNowOffline(wfsServers,startTime)
		}{
			this.playNowClient(wfsServers,startTime)
		}
	}

	playNowOffline { |wfsServers, startTime = 0, syncCenter |
		SyncCenter.localSync({ |syncCenter|
			var nodeID, serverIndex, servers;
		
			#serverIndex, servers =  
				wfsServers.nextArray( this.typeActivity );
	
			this.prepareForPlayback;
						 
			WFS.debug( "% - s:%, %", WFS.secsToTimeCode( startTime ),serverIndex,
				filePath );
					
			this.load( servers, syncCenter )
		});		
	}

	playNowClient { |wfsServers, startTime = 0, syncCenter |
		SyncCenter.localSync({ |syncCenter|
			var nodeID, serverIndex, servers;
			
			//if( this.intType == \switch ) { "playing switch".postln; };
	
			wfsServers = wfsServers ? WFSServers.default; 
			
			if( prefServer.notNil )
				{ servers = [ wfsServers.nextDictServer(  this.typeActivity, prefServer ) ];  }
				{ servers = wfsServers.nextDictServers( this.typeActivity );  };
				
			if( this.useSwitch && (this.intType != \switch) )
				{ this.copyNew
					.intType_( \switch ).playNowClient( wfsServers, startTime ); };
			
			this.prepareForPlayback;
						 
			WFS.debug( "% - s:%, %", WFS.secsToTimeCode( startTime ), serverIndex, filePath );
						
			this.load( servers, syncCenter );
		})
	}		
			
		
	*generateSynth { |wfsDefName, wfsPath, servers, delayBuffer, sfBuffer, pbRate = 1, level = 1, loop = 1,
		dur = 5, input = 0, args, fadeTimes, wfsPathStartIndex = 0, delta = 1, syncCenter|
		
		var sfBufNum = 0, wfsDefIntType;
		var allArgs, localConfSizes, indexIndex, indexUse;
		
		fadeTimes = fadeTimes ? [0,0];
		
		wfsDefIntType = wfsDefName.wfsIntType;
		if( sfBuffer.notNil ) { sfBufNum = sfBuffer.asCollection.collect( _.bufnum ) };
		
		if( wfsDefIntType === 'index' ) {
			 localConfSizes = ( WFSServers.default.wfsConfigurations !? 
					{ WFSServers.default.wfsConfigurations.collect( _.size ) } )
					? [ 96, 96 ]; 
			 indexIndex = wfsPath % localConfSizes;
			 localConfSizes = [0] ++ localConfSizes.integrate;
			 indexUse = localConfSizes[1..].collect({ |item, i|
			 		(( wfsPath >= localConfSizes[i]) && ( wfsPath < item )).binaryValue
			 		});
		};
			
		
		case { wfsDefIntType == 'static' }
			{ 	allArgs = 
				[	\i_x, wfsPath.x,
					\i_y, wfsPath.y,
					\i_z, wfsPath.z,									\bufD, delayBuffer.asCollection.collect( _.bufnum ),
					\bufP, sfBufNum,
					\input, input,
					\totalTime, dur,
					\level, level,
					\loop, loop,
					\rate, pbRate,
					\outOffset, outOffset,
					\i_fadeInTime, fadeTimes.wrapAt(0),
					\i_fadeOutTime, fadeTimes.wrapAt(1) ]; }			{ wfsDefIntType == 'plane' }
			{ allArgs =
				[	\i_a, wfsPath.angle,
					\i_d, wfsPath.distance,
					\bufD, delayBuffer.asCollection.collect( _.bufnum ),
					\bufP, sfBufNum,
					\input, input,
					\totalTime, dur,
					\level, level,
					\loop, loop,
					\rate, pbRate,
					\outOffset, outOffset,
					\i_fadeInTime, fadeTimes.wrapAt(0),
					\i_fadeOutTime, fadeTimes.wrapAt(1)
					  ]; }
			{ wfsDefIntType == 'index' }
				 { allArgs =
					[	\i_index, indexIndex,
						\i_use, indexUse,
						// \bufD, delayBuffer.bufnum,
						\bufP, sfBufNum,
						\input, input,
						\totalTime, dur,
						\level, level,
						\loop, loop,
						\rate, pbRate,
						\outOffset, outOffset,
						\i_fadeInTime, fadeTimes.wrapAt(0),
						\i_fadeOutTime, fadeTimes.wrapAt(1)  ]; }
			{ true }
			{  allArgs = 
				[	\bufT , wfsPath.timesBuffer.asCollection.collect( _.bufnum ),
					\bufXYZ, wfsPath.positionsBuffer.asCollection.collect( _.bufnum ),
					\bufD, delayBuffer.asCollection.collect( _.bufnum ),
					\path_startIndex, wfsPathStartIndex,
					\bufP, sfBufNum,
					\input, input,
					\totalTime, dur,
					\rate, pbRate,
					\level, level,
					\loop, loop,
					\outOffset, outOffset,
					\i_fadeInTime, fadeTimes.wrapAt(0),
					\i_fadeOutTime, fadeTimes.wrapAt(1)  ];
			};
					
		^Synth.newWFS( wfsDefName, allArgs ++ args, servers, delta: delta ); 
	}
	
	*freeAllBuffers { loadedSynths.do({ |item| item.freeBuffers }) }
	*freeAllSynths { loadedSynths.do( _.free ); }
	
	}
	
+ Synth {
	
	*newWFS { arg defName, args, servers, addAction=\addToHead, delta = 1, syncCenter;
		var synths, addNum, inTargets, nodeID;
		
		//wfsServers = wfsServers ? WFSServers.default;
		servers = servers.asCollection;
			
		inTargets = servers.collect( _.asTarget );
		addNum = addActions[addAction];
		nodeID = servers.collect(_.nextNodeID);
		
		//"newSynth nodeID: %\n".postf( nodeID );

		synths = servers.collect({ |server,i| 
			this.basicNew(defName, server, nodeID[i]); 
		});
			
		if (addNum < 2)
			{ synths.do({ |synth, i|
				synth.group = inTargets[i]; }); }
			{ synths.do({ |synth, i|
				synth.group = inTargets[i].group; }); };
		
		servers.do({ |server, i|
			server.sendSyncedBundle(delta, syncCenter,
				[9, defName, nodeID[i], addNum, inTargets[i].nodeID] ++ 
				args.atArgValue( i ) ++
				WFSEQ.currentArgsDict.asArgsArray 
			); // "s_new"
		});
		
		^synths
	}	
}

+ Collection {
	atArgValue { |index = 0|
			
		// [ \freq, [440, 330], \level, 1 ].atArgValue(1) 
		//  returns: [ \freq, 330, \level, 1 ]
		
		var out;
		out = [];
		this.pairsDo({ |a,b|
			out = out ++ [ a, b.asCollection.wrapAt( index ) ];
			});
		^out;
	}
}