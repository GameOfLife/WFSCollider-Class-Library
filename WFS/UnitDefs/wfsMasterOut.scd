MultiChannelUdef( \wfsMasterOut, { |bus = 0, toServers = 1, u_busOffset = 14|
	var minBus, maxBus, useGlobalGain;
	minBus = (u_busOffset * toServers);
	maxBus = if( toServers, NumOutputBuses.ir - 1, u_busOffset - 1 );
	bus = bus + minBus;
	useGlobalGain = 1 - toServers;
	bus = bus * InRange.kr( bus, minBus, maxBus );
	
	Out.ar( bus, UIn.ar( 0, Udef.numChannels ) * UEnv.kr( useGlobalGain: useGlobalGain ) );
}, channels: (1..8))
	.category_( \wfs_io )
	.setSpec( \u_busOffset, PositiveIntegerSpec( 14 ) )
	.setSpec( \bus, PositiveIntegerSpec( 0 ) )
	.setSpec( \toServers, BoolSpec( true ) )
	.shouldPlayOnFunc_({ |unit, target|
		target.asTarget.server == WFSServers.default.m;
	});