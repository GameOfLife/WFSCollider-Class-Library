// a pseudo-ugen to play a BufSndFile from inside a Unit.
// auto-creates a control with the name <key> (default: bufSoundFile), format:
//	[ bufnum, rate, loop ]

BufSndFilePlayer {
	
	*ar { |numChannels = 1, key, startPos = 0, trigger = 1|
		var bufnum, rate, loop, startOffset;
		key = key ? 'soundFile';
		#bufnum, rate, loop = key.asSymbol.kr( [ 0, 1, 0 ] );
		startOffset = 'u_startOffset'.kr(0); // for use inside a U or UChain
		startPos = ((startOffset * BufSampleRate.kr( bufnum )) / rate) + startPos;
		^PlayBuf.ar( numChannels, bufnum, BufRateScale.kr( bufnum ) * rate, 
			trigger, startPos, loop );
	}
	
	*kr { |numChannels = 1, key, startPos = 0, trigger = 1| // u_startOffset not correct yet
		var bufnum, rate, loop, startOffset;
		key = key ? 'soundFile';
		#bufnum, rate, loop = key.asSymbol.kr( [ 0, 1, 0 ] );
		startOffset = 'u_startOffset'.kr(0); // for use inside a U or UChain
		startPos = ((startOffset * BufSampleRate.kr( bufnum )) / rate) + startPos;
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
		key = key ? 'soundFile';
		#bufnum, rate, loop = key.asSymbol.kr( [ 0, 1, 0 ] );
		^VDiskIn.ar( numChannels, bufnum, BufRateScale.kr( bufnum ) * rate, loop );
	}
	
}