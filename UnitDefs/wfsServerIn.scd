Udef( \wfsServerIn, { |bus = 0| // same as soundIn, but only plays on servers 
	var input;
	input = SoundIn.ar( bus );
	input = input * if( bus > (NumInputBuses.ir - 1), 0, 1 ); // silent if bus out of range
	UOut.ar( 0, input );
})
	.category_( \wfs_io )
	.setSpec( \bus, PositiveIntegerSpec( 0 ) )
	.shouldPlayOnFunc_({ |unit, target|
		WFSSpeakerConf.allServers.includes( target.asTarget.server );
	});