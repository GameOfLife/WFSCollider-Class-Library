SyncCenter {	
	classvar <>mode;
	
	classvar <>servers, <>master;
	classvar <>recSynths;
	classvar <>remoteCounts, <>masterCount, <>masterCountTime;
	classvar <>oldRemoteCounts, <>oldMasterCount, <>oldMasterCountTime;
	classvar <>responder;
	classvar <>global;
	
	classvar <>inBus = -28, <>outBus = 0;
	
	var <>localCount, <>localTime;
	
	*testSampleSched { // test whether this is a sample-sched enabled version of SC
		^'BlockCount'.asClass.notNil;
		}
	
	*initClass {
		if( this.testSampleSched )
			{ mode = 'sample'; }
			{ mode = 'timestamp'; };
		servers = [];
		remoteCounts = Order();	
		global = this.new;
		}
	
	*add { |server|
		if( server.isKindOf( Server ).not )
			{ "%.add: added item should be a Server".format( this ).warn; }
			{
			if( servers.includes( server ) )
				{ "%.add: server '%' was already added".format( this, server ).warn; }
				{ servers = servers.add( server ); }
			};
		}
		
	*addAll { |array| array.do( this.add( _ ) ); }
	
	*recDef { ^SynthDef( "sync_receive", { |id = 100, in = 0|
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
				SendTrig.ar( trig, id, SampleOffset.ir );
				FreeSelf.kr( T2K.kr( trig ) );
				});
			}
	
	*sendDefs { |write = false|
		var recdev;
		if( mode === 'sample' )
			{
			recdev = this.recDef;
				
			if( write )
				{ servers.do({ |server| recdev.load( server ) }); }
				{ servers.do({ |server| recdev.send( server ) }); };
			
			if( write )
				{ this.masterDef.load( master ) }
				{ this.masterDef.send( master ) };
			}
		}
		
	*playRecDefs { 
		var recdev;
		if( mode === 'sample' )
			{
			recdev = this.recDef;
			recSynths = recSynths.addAll( 
				servers.do({ |server, i| recdev.play( server, 
						[\id, 100+i, \in, inBus] ).register }) ); 
			}
		}
		
	*remoteSync { |waitTime = 0.5|
		if( mode === 'sample' )
			{ 
			oldRemoteCounts = remoteCounts;
			oldMasterCount = masterCount;
			oldMasterCountTime = masterCountTime;
			
			remoteCounts = Order();
			masterCount = nil;
			
			if( responder.notNil )
				{ responder.remove; };
				
			responder = OSCresponderNode( nil, '/tr', { |time, resp, msg|
					case{ msg[ 2 ] == 99 }
						{ masterCount = (msg[4] * master.blockSize) + msg[3]; }
						{ msg[2].exclusivelyBetween( 99, 100 + servers.size ) }
						{ remoteCounts[ msg[2] - 100 ] = 
							(msg[3] * master.blockSize) + msg[4] };
							
					if( (remoteCounts.size >= servers.size) && masterCount.notNil )
						{ resp.remove; responder = nil };
					}).add;
			
			this.masterDef.send( master );
			this.playRecDefs;
			
			{
			masterCountTime = thisThread.seconds + waitTime;
			Synth.sched( waitTime, "sync_master", nil, master, \addToHead ); 
			}.r.play;
			
			};
		}
		
	*localSync {
		
		
		}
	
	
	
	
	}
	