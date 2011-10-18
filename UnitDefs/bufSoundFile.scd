(
var udefs;

udefs = [1,2,3,4,5,6,7,8,10,12,16,24,32].collect({ |numChannels|
	HiddenUdef( "bufSoundFile_%".format( numChannels ).asSymbol, { |amp = 1|
		UOut.ar( 0, BufSndFilePlayer.ar( numChannels ) * amp, true );
	}, [ [ \soundFile, nil, BufSndFileSpec(nil) ] ], \soundFile )
});

MultiUdef( \bufSoundFile, udefs, \soundFile )
	.chooseFunc_({ |args|
		var sf, numChannels;
		sf = (args ? []).pairsAt( \soundFile );
		if( sf.notNil ) { 
			"bufSoundFile_%".format( sf.numChannelsForPlayBuf ).asSymbol;
		} {
			\bufSoundFile_1
		};
	});

)