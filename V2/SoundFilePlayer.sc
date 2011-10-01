// a pseudo-ugen to play a BufSndFile from inside a Unit.
// auto-creates a control with the name <key> (default: bufSoundFile), format:
//	[ bufnum, rate, loop ]

BufSndFilePlayer {
	
	// *getArgs may also be used to feed other buffer playing UGens
	//	the list is formed according to PlayBuf arguments
	
	*getArgs { |numChannels = 1, key, trigger = 1, startPos, ugenRate = \audio| // user may override startPos
		var bufnum, rate, loop, startFrame;
		key = key ? 'soundFile';
		#bufnum, rate, loop = key.asSymbol.kr( [ 0, 1, 0 ] );
		if( startPos.isNil ) { startPos = 'u_startPos'.kr(0); }; // for use inside a U or UChain
		startFrame = ((startPos * BufSampleRate.kr( bufnum )) / rate);
		if( ugenRate == \control ) { startFrame = startFrame / (SampleRate.ir / ControlRate.ir); };
		^[ numChannels, bufnum, BufRateScale.kr( bufnum ) * rate, trigger, startFrame, loop ];
	}
	
	*ar { |numChannels = 1, key, trigger = 1, startPos, doneAction = 0|
		^PlayBuf.ar( *this.getArgs( numChannels, key, trigger, startPos ) ++ [ doneAction ] );
	}
	
	*kr { |numChannels = 1, key, trigger = 1, startPos, doneAction = 0|
		^PlayBuf.kr( *this.getArgs( numChannels, key, trigger, startPos, \control ) ++ [ doneAction ]  );
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