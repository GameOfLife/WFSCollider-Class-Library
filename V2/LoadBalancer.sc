// a LoadBalancer holds a set of servers and their approximate cpu loads

LoadBalancer {
	
	classvar <>all;
	
	var <servers;
	var <>loads;
	
	*initClass { all = [] }
	
	*new { |...servers|
		^super.newCopyArgs( servers ).init.addToAll;
	}
	
	init {
		loads = 0!servers.size;
	}
	
	indexOf { |server|
		^servers.indexOf( server );
	}
	
	addServer { |server|
		if( servers.includes( server ).not ) {
			servers = servers.add( server );
			loads = loads.add(0);
		};
	}
	
	removeServer { |server|
		var index;
		index = this.indexOf( server );
		if( index.notNil ) {
			servers.removeAt( index );
			loads.removeAt( index );
		} {
			^nil
		};
	}
	
	addToAll {
		if( all.includes( this ).not ) {
			all = all.add( this );
		};
	}
	
	removeFromAll {
		all.remove( this );
	}
	
	addLoad { |server, load = 0|
		var index;
		index = this.indexOf( server );
		if( index.notNil ) {
			loads[ index ] = loads[ index ] + load;
		};
	}
	
	setLoad { |server, load = 0|
		var index;
		index = this.indexOf( server );
		if( index.notNil ) {
			loads[ index ] = load;
		};
	}
	
	reset { this.init }
	
	lowest {
		^servers[ loads.minIndex ];
	}
	
	asTarget { |addLoad = 0|
		var server;
		server = this.lowest;
		this.addLoad( server, addLoad );
		^server.asTarget;
	}
	
	doesNotUnderstand { |selector ...args|
		servers[0] !? { 
			servers[0].perform( selector, *args );
		};
	}
}

+ Server {
	
	loadBalancerAddLoad { |load = 0|
		LoadBalancer.all.do({ |lb|
			lb.addLoad( this, load );
		});
	}
	
	loadBalancerSetLoad { |load = 0|
		LoadBalancer.all.do({ |lb|
			lb.setLoad( this, load );
		});
	}
	
}