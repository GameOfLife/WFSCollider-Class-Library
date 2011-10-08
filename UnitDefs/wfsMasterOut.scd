Udef( \wfsMasterOut, { |bus = 0, toServers = 1, u_busOffset = 14|
	var minBus, maxBus;
	minBus = (u_busOffset * toServers);
	maxBus = if( toServers, NumOutputBuses.ir - 1, u_busOffset - 1 );
	bus = bus + minBus;
	
	bus = bus * InRange.kr( bus, minBus, maxBus );
		
	Out.ar( bus, UIn.ar( 0 ) * UEnv.kr );
})
	.category_( \wfs_io )
	.setSpec( \u_busOffset, PositiveIntegerSpec( 14 ) )
	.setSpec( \bus, PositiveIntegerSpec( 0 ) )
	.setSpec( \toServers, BoolSpec( true ) )
	.shouldPlayOnFunc_({ |unit, target|
		target.asTarget.server == WFSServers.default.m;
	});