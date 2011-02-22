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
	classvar <>mode;
	
	classvar <>servers, <>master;
	classvar <>recSynths;
	classvar <>remoteCounts, <>masterCount, <>masterCountTime;
	classvar <>oldRemoteCounts, <>oldMasterCount, <>oldMasterCountTime;
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
		servers = [];
		remoteCounts = Order();	
		global = this.new;
		masterCount = Ref(-1);
		ready = Ref(false);
		serverControllers !? { serverControllers.do(_.remove) };
		serverControllers = List.new;
		
	}
	
	*add { |server|
		var index;
		if( server.isKindOf( Server ).not ) { 
				"%.add: added item should be a Server".format( this ).warn; 
		} {
			if( servers.includes( server ) ) { 
				"%.add: server '%' was already added".format( this, server ).warn; 
			} { 
				servers = servers.add( server );
				remoteCounts.put(servers.size-1,Ref(-1));
				index = remoteCounts.size - 1;
				serverControllers.add(
					Updater(server, { |name,msg|
						if(msg == \serverRunning) {
							remoteCounts[index].value_(-1).changed;
						}
					})
				)
							
			}
		};
	}
		
	*addAll { |array| array.do( this.add( _ ) ); }
	
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
	
	*sendDefs { |write = false|
		var recdev;
		if( mode === 'sample' ) {
			recdev = this.recDef;
				
			if( write ) {
				servers.do({ |server| recdev.load( server ) });
				this.masterDef.load( master );
			} { 
				servers.do({ |server| recdev.send( server ) });
				this.masterDef.send( master )
			};
		}
	}
		
	*playRecDefs { 
		var recdev;
		if( mode === 'sample' ) {
			recdev = this.recDef;
			recSynths = recSynths.addAll( 
				servers.collect({ |server, i|  
					("playing receve sync synthdef for server "++i).postln; 
					Synth( "sync_receive", [\id, 100+i,\in,inBus], server ).register
				}) 
			); 
		}
	}
		
	*remoteSync { |waitTime = 0.5|
		var counter=0;
		ready.value_(false).changed;
		if( mode === 'sample' ) { 
			oldRemoteCounts = remoteCounts;
			oldMasterCount = masterCount;
			oldMasterCountTime = masterCountTime;
					
			if( responder.notNil ) {
				responder.remove; "removing responder".postln 
			};
			
			masterCount.value_(-1).changed;
			remoteCounts.do{ |c| c.value_(-1).changed };
				
			responder = OSCresponderNode( nil, '/tr', { |time, resp, msg|
				("Received: "++[time, resp, msg] ).postln;
				case{ msg[ 2 ] == 99 }
					{ masterCount.value_( (msg[4] * master.options.blockSize) + msg[3]) .changed; "setting master counts".postln; }
					{ msg[2].exclusivelyBetween( 99, 100 + servers.size ) }
					{ 
						("serverNumber "++( msg[2] - 100 ) ).postln;
						remoteCounts[ msg[2] - 100 ].value_( (msg[4] * servers[ msg[2] - 100 ].options.blockSize) + msg[3] ).changed;
						("Setting remoteCounts for server "++(msg[2] - 100)++" : "++ remoteCounts[ msg[2] - 100 ].value).postln;	
					 }; 
						
				if( 
					remoteCounts.collect{ |v,i| if( servers[i].serverRunning ) { v.value != -1 } { true }  }.reduce('&&') 
					&& (masterCount.value != -1) 
				) { 
					resp.remove; responder = nil; "received all counts".postln; ready.value_(true).changed;
				};
			}).add;
			"adding responder".postln;				
			
			//this.masterDef.send( master );
			this.playRecDefs;
			
			{
				masterCountTime = thisThread.seconds + waitTime;
				Synth.sched( waitTime, "sync_master", [\out, outBus], master, \addToHead );
				"playing impulse".postln;
				while({counter < 5}){
					0.1.wait;
					counter = counter + 0.1;
				};
				if(ready.value){ "Sync successful!".postln}{"No Sync".postln};
				responder.remove;
				responder = nil;
				recSynths.do({ |synth| if(synth.isPlaying){synth.free} });
				recSynths = nil;
				
			}.r.play;
		};
	}
		
	*localSync {
		
		
	}
	
	*getSchedulingSampleCount{ |delta = 1, serverIndex = 0|
		^(remoteCounts[serverIndex].value + (((master.options.blockSize*master.blockCount) + (delta*master.sampleRate)) - masterCount.value))
	}
	
	*getSchedulingSampleCountS{ |delta = 1, server|
		var serverIndex = servers.indexOf(server).postln;
		^this.getSchedulingSampleCount(delta,serverIndex);
	}
	
	*sendPosOSCBundleAll{ |delta = 1, bundle|
		servers.do{ |server| this.sendPosBundle(delta,bundle,server) }
	}

}
	