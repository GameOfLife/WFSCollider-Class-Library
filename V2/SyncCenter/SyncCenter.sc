/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2010-2011 Wouter Snoei, Miguel Negr‹o.

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

SyncCenter {	
	classvar <>mode, <>verbose = false;
	
	classvar <>serverCounts, <master;
	classvar <>recSynths;
	classvar <>masterCountTime;
	classvar <>responder;
	classvar <>global;
	classvar <>ready;
	classvar <>serverControllers;
	classvar <>inBus = 0, <>outBus = 14;
	
	var <>localCount, <>localTime;
	
	*testSampleSched { // test whether this is a sample-sched enabled version of SC
		^'BlockCount'.asClass.notNil;
	}
	
	*initClass {
		
		if( this.testSampleSched ) {
			mode = 'sample'; 
		} { 
			mode = 'timestamp'; 
		};
		serverCounts = Dictionary.new;
		global = this.new;
		ready = Ref(false);
		serverControllers !? { serverControllers.do(_.remove) };
		serverControllers = List.new;
		
	}
	
	*servers{ ^serverCounts.keys.select({ |sv| sv != master }).as(Array) }
	
	*add { |server|
		var index;
		if( server.isKindOf( Server ).not ) { 
				"%.add: added item should be a Server".format( this ).warn; 
		} {
			if( this.servers.includes( server ) ) { 
				"%.add: server '%' was already added".format( this, server ).warn; 
			} { 
				serverCounts.put(server,Ref(-1));
				serverControllers.add(
					Updater(server, { |name,msg|
						if(msg == \serverRunning) {
							serverCounts.at(server).value_(-1).changed;
						}
					})
				)
							
			}
		};
	}
		
	*addAll { |array| array.do( this.add( _ ) ); }
	
	*master_ { |server|
		master = server;
		this.add(server);
	}
	
	*recDef { 
		^SynthDef( "sync_receive", { |id = 100, in = 0 |
			// waits for an impulse
			var trig;
			trig = Trig1.ar( SoundIn.ar( in ) );
			SendTrig.ar( trig, id, BlockOffset.ar( trig ) );
			//FreeSelf.kr( T2K.kr( trig ) );
		});
	}
			
	*masterDef { 
		^SynthDef( "sync_master", { |out = 0, amp = 0.1, id = 99|
			var trig;
			trig = OneImpulse.ar;
			OffsetOut.ar( out, trig );
			SendTrig.ar( trig, id, SpawnOffset.ir );
			//FreeSelf.kr( T2K.kr( trig ) );
		});
	}
	
	*writeDefs{
		this.recDef.writeDefFile;
		this.masterDef.writeDefFile;
	}
	
	*loadMasterDef{
		this.masterDef.load( master )
	}
	
	*sendDefs { |write = false|
		var recdev;
		if( mode === 'sample' ) {
			recdev = this.recDef;
				
			if( write ) {
				this.servers.do({ |server| recdev.load( server ) });
				this.masterDef.load( master );
			} { 
				this.servers.do({ |server| recdev.send( server ) });
				this.masterDef.send( master )
			};
		}
	}
		
	*playRecDefs { 
		var recdev;
		if( mode === 'sample' ) {
			recdev = this.recDef;
			recSynths = recSynths.addAll( 
				this.servers.collect({ |server, i|  
					if(verbose) { ("playing receve sync synthdef for server "++i).postln };
					Synth( "sync_receive", [\id, 100+i,\in,inBus], server ).register
				}) 
			); 
		}
	}
		
	*remoteSync { |waitTime = 0.5|
		var counter=0;
		ready.value_(false).changed;
		if( mode === 'sample' ) { 

			if( responder.notNil ) {
				responder.remove; 
				if( verbose ) { "removing responder".postln }
			};
			
			serverCounts.pairsDo{ |server,count|
				count.value_(-1).changed
			};
				
			responder = OSCresponderNode( nil, '/tr', { |time, resp, msg|
				var server, numOfBlocks, offsetInsideBlock, count;
				if( verbose ) { ("Received: "++[time, resp, msg] ).postln };
				case{ msg[ 2 ] == 99 }
					{ 
						numOfBlocks = msg[4];
						offsetInsideBlock = msg[3];
						
						serverCounts.at(master).value_( (numOfBlocks * master.options.blockSize) + offsetInsideBlock).changed; 
						
						if( verbose ) { "setting master counts".postln }; 
					}
					{ msg[2].exclusivelyBetween( 99, 100 + this.servers.size ) }
					{ 
						server = this.servers[msg[2] - 100];
						numOfBlocks = msg[4];
						offsetInsideBlock = msg[3];
						count = (numOfBlocks * server.options.blockSize) + offsetInsideBlock;
						
						serverCounts.at(server).value_( count ).changed;
						
						if(verbose) { 
							("serverNumber "++( msg[2] - 100 ) ).postln;
							("Setting remoteCounts for server "++(msg[2] - 100)++" : "++ count ).postln;
						};	
					 }; 
						
				if( 
					serverCounts.collect{ |count,server| if( server.serverRunning ) { count.value != -1 } { true } }
					.as(Array).reduce('&&')

				) { 
					resp.remove; responder = nil;
					ready.value_(true).changed;
					if(verbose) { "received all counts".postln;  }
				};
			}).add;
			
			this.playRecDefs;
			
			{
				masterCountTime = thisThread.seconds + waitTime;
				Synth.sched( waitTime, "sync_master", [\out, outBus], master, \addToHead );
				if( verbose ) { "playing impulse".postln };
				while({counter < 5}){
					0.1.wait;
					counter = counter + 0.1;
				};
				if(verbose) {
					if(ready.value){ "Sync successful!".postln}{ "No Sync".postln };
				};
				responder.remove;
				responder = nil;
				recSynths.do({ |synth| if(synth.isPlaying){synth.free} });
				recSynths = nil;
				
			}.r.play;
		};
	}
		
	*localSync {
		
		
	}
	
	*getSchedulingSampleCountS{ |delta = 1, server|
		if( server == master ) {
			^(master.options.blockSize*master.blockCount) + (delta*master.sampleRate)
		} {
			^serverCounts.at(server).value + (master.options.blockSize*master.blockCount) + (delta*master.sampleRate) - serverCounts.at(master).value
		}
	}
	
	*makeWindow {
		SyncCenterGui.new
	}

}
	