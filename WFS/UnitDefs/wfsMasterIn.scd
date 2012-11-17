MultiChannelUdef( \wfsMasterIn, { |bus = 0|
	var input;
	input = ((..Udef.numChannels - 1) + bus).collect({ |item| SoundIn.ar( item ) });
	input = input.collect({ |input|
		input * if( bus > (NumInputBuses.ir - 1), 0, 1 );
	});
	UOut.ar( 0, input );
} ).setSpec( \bus, PositiveIntegerSpec( 0 ) )
	.category_( \wfs_io )
	.shouldPlayOnFunc_({ |unit, target|
		target.asTarget.server == WFSServers.default.m;
	});