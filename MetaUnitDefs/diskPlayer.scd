MetaUdef(\diskPlayer, { |numChannels = 1|
	{ |amp = 1|
		UOut.ar( 0, DiskSndFilePlayer.ar( numChannels ) );
	}
},[\numChannels,1])
