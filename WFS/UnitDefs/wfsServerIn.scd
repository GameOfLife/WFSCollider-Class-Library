(
var def;
def = MultiChannelUdef( \wfsServerIn, { |bus = 0| // same as soundIn, but only plays on servers 
	var input;
	input = ((..Udef.numChannels - 1) + bus).collect({ |item| SoundIn.ar( item ) });
	input = input.collect({ |input|
		input * if( bus > (NumInputBuses.ir - 1), 0, 1 );
	});
	UOut.ar( 0, input );
}, channels: (1..8))
	.category_( \wfs_io )
	.shouldPlayOnFunc_({ |unit, target|
		WFSSpeakerConf.includesServer( target.asTarget.server );
	});

def.udefs.do({ |item|
	item.setSpec( \bus, IntegerSpec( 0, 0, 8 - item.numChannels ) );
});

def;
)