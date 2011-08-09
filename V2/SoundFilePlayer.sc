// a pseudo-ugen to play a BufSndFile from inside a Unit.
// auto-creates a control with the name <key> (default: bufSoundFile), format:
//	[ bufnum, rate, loop, startPos ]

BufSndFilePlayer {
	
	*ar { |numChannels = 1, key, trigger = 1|
		var bufnum, rate, loop, startPos;
		key = key ? 'bufSoundFile';
		#bufnum, rate, loop, startPos = key.asSymbol.kr( [ 0, 1, 0, 0 ] );
		^PlayBuf.ar( numChannels, bufnum, BufRateScale.kr( bufnum ) * rate, 
			trigger, startPos, loop );
	}
	
	*kr { |numChannels = 1, key, trigger = 1|
		var bufnum, rate, loop, startPos;
		key = key ? 'bufSoundFile';
		#bufnum, rate, loop, startPos = key.asSymbol.kr( [ 0, 1, 0, 0 ] );
		^PlayBuf.kr( numChannels, bufnum, BufRateScale.kr( bufnum ) * rate, 
			trigger, startPos, loop );
	}
	
}


// a pseudo-ugen to play a DiskSndFile from inside a Unit.
// auto-creates a control with the name <key> (default: diskSoundFile), format:
//	[ bufnum, rate, loop ]

DiskSndFilePlayer {
	
	*ar { |numChannels = 1, key|
		var bufnum, rate, loop;
		key = key ? 'diskSoundFile';
		#bufnum, rate, loop = key.asSymbol.kr( [ 0, 1, 0 ] );
		^VDiskIn.ar( numChannels, bufnum, BufRateScale.kr( bufnum ) * rate, loop );
	}
	
}