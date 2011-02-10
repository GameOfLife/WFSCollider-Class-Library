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
	classvar <>ready = false;
	
	classvar <>inBus = -28, <>outBus = 0;
	
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
	}
	
	*add { |server|
		if( server.isKindOf( Server ).not ) { 
				"%.add: added item should be a Server".format( this ).warn; 
		} {
			if( servers.includes( server ) ) { 
				"%.add: server '%' was already added".format( this, server ).warn; 
			} { 
				servers = servers.add( server ); 
			}
		};
	}
		
	*addAll { |array| array.do( this.add( _ ) ); }
	
	*recDef { ^SynthDef( "sync_receive", { |id = 100, in = 0 |
		// waits for an impulse
		var trig;
		trig = Trig1.ar( SoundIn.ar( in ) );
		SendTrig.ar( trig, id, BlockOffset.ar( trig ) );
		FreeSelf.kr( T2K.kr( trig ) );
		});
	}
			
	*masterDef { ^SynthDef( "sync_master", { |out = 0, amp = 0.1, id = 99|
		var trig;
		trig = OneImpulse.ar;
		OffsetOut.ar( out, trig );
		SendTrig.ar( trig, id, SampleOffset.ir.poll );
		FreeSelf.kr( T2K.kr( trig ) );
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
				servers.collect({ |server, i|  ("playing receve sync synthdef for server "++i).postln; 
				recdev.play( server, [\id, 100+i,\in,inBus] ).register }) 
			); 
		}
	}
		
	*remoteSync { |waitTime = 0.5|
		var counter=0;
		ready = false;
		if( mode === 'sample' ) { 
			oldRemoteCounts = remoteCounts;
			oldMasterCount = masterCount;
			oldMasterCountTime = masterCountTime;
			
			remoteCounts = Order();
			masterCount = nil;
			
			if( responder.notNil ) {
				responder.remove; "removing responder".postln 
			};
				
			responder = OSCresponderNode( nil, '/tr', { |time, resp, msg|
				("Received: "++[time, resp, msg] ).postln;
				case{ msg[ 2 ] == 99 }
					{ masterCount = (msg[4] * master.options.blockSize) + msg[3]; "setting master counts".postln; }
					{ msg[2].exclusivelyBetween( 99, 100 + servers.size ) }
					{ remoteCounts[ msg[2] - 100 ] = 
						//(msg[4] * master.options.blockSize) + msg[3]; 
						(msg[4] * servers[ msg[2] - 100 ].options.blockSize) + msg[3];//shouldn't it be relative to slave blocksize ?
					("Setting remoteCounts for server "++(msg[2] - 100)++" : "++ remoteCounts[ msg[2] - 100 ]).postln;	
					 }; 
						
				if( (remoteCounts.size >= servers.size) && masterCount.notNil ) { 
					resp.remove; responder = nil; "received all counts".postln; ready = true;
				};
			}).add;
			"adding responder".postln;				
			
			this.masterDef.send( master );
			this.playRecDefs;
			
			{
				masterCountTime = thisThread.seconds + waitTime;
				Synth.sched( waitTime, "sync_master", [\out, outBus], master, \addToHead );
				"playing impulse".postln;
				while({counter < 5}){
					0.1.wait;
					counter = counter + 0.1;
				};
				if(ready){ "Sync successful!".postln}{"No Sync".postln};
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
		^(remoteCounts[serverIndex] + (((master.options.blockSize*master.blockCount) + (delta*master.sampleRate)) - masterCount))
	
	}
	
	*getSchedulingSampleCountS{ |delta = 1, server|
		var serverIndex = servers.indexOf(server);
		
		^this.getSchedulingSampleCount(delta,serverIndex);
	}
	
	*sendPosBundle{ |delta = 1, bundle,server|
		bundle.sendPos(server,this.getSchedulingSampleCountS(delta,server)) 
	}
	
	*sendPosBundleAll{ |delta = 1, bundle|
		servers.do{ |server| this.sendPosBundle(delta,bundle,server) }
	}

}
	