CPULoadBalancer {
	// system for keeping track of where synths should go on multi-server systems
	// if loads are equal, it will choose the least popular (i.e. the least chosen)
	
	classvar <>apxCPUs, <>groups;
	classvar <>popularity;
	
	*initClass {
		this.clear;
		CmdPeriod.add( this );
	}
	
	*clear {
		apxCPUs = ();
		groups = ();
		popularity = ();
	}
	
	*addServer { |server, group = 0|
		apxCPUs[ server ] = apxCPUs[ server ] ? 0;
		groups[ server ] = group; // overwrite previous group
	}
	
	*removeServer { |server|
		apxCPUs.removeAt( server );
		groups.removeAt( server );
	}
	
	*addCPU { |server, amt = 1|
		if( apxCPUs[ server ].isNil ) { this.addServer(server) };
		apxCPUs[ server ] = apxCPUs[ server ] + amt; 
	}
	
	*removeCPU { |server, amt = 1|
		if( apxCPUs[ server ].isNil ) { this.addServer(server) };
		apxCPUs[ server ] = apxCPUs[ server ] - amt;
	}
	
	*setCPU { |server, amt = 0|
		if( server.isNil ) {
			apxCPUs.keys.do( this.setCPU(_, amt) );
		} {
			if( apxCPUs[ server ].isNil ) { this.addServer(server) };
			apxCPUs[ server ] = amt;
		};	
	}
	
	*getCPU { |server|
		^apxCPUs[ server ] ? 0;
	}
	
	*getGroup { |server|
		^groups[ server ] ? 0;
	}
	
	*lowest { |group = 0|
		var choice;
		choice = groups.select( _ == group ).keys
			.as(Array)
			.sort({ |a,b| a.name <= b.name }) // sort by name
			.sort({ |a,b| 
				var ca, cb;
				ca = apxCPUs[a];
				cb = apxCPUs[b];
				if( ca != cb ) { 
					ca <= cb; // first sort on cpu
				} { 
					(popularity[a] ? 0) <= (popularity[b] ? 0); // then on popularity
				};
			})[0];
		if( choice.notNil ) { 
			popularity[ choice ] = (popularity[ choice ] ? 0) + 1;
		};
		^choice;
	}
	
	*resetPopularity {
		popularity = ();
	}
	
	*reset {
		this.setCPU(nil,0);
		this.resetPopularity;
	}
	
	*cmdPeriod {
		this.reset;
	}
	
}


+ Server {
	apxCPU { ^CPULoadBalancer.getCPU( this ); }
	
	addApxCPU { |amt = 1| CPULoadBalancer.addCPU( this, amt ); }
	removeApxCPU { |amt = 1| CPULoadBalancer.removeCPU( this, amt ); }
	setApxCPU { |amt = 0| CPULoadBalancer.setCPU( this, amt ); }
	getApxCPU { ^CPULoadBalancer.getCPU( this ); }
	
	setApxCPUGroup { |group = 0| CPULoadBalancer.addServer( this, group ) }
	getApxCPUGroup { ^CPULoadBalancer.getGroup( this ) }
	
	*lowestApxCPU { |group = 0| ^CPULoadBalancer.lowest( group ) } 
	lowestApxCPU { ^this.class.lowestApxCPU( this.getApxCPUGroup ) } // from same group
	
	*nextLowestCPU { |addAmt = 1, group = 0|
		var srv;
		if( group.class == Server ) {
			srv = group;
			CPULoadBalancer.addServer(srv);
			group = srv.getApxCPUGroup;
		};
		srv = this.lowestApxCPU( group ) ? srv;
		srv.addApxCPU( addAmt );
		^srv;
	}
	nextLowestCPU { |addAmt = 1| ^this.class.nextLowestCPU( addAmt, this ); }
}