MetaUdef(\soundFilePlayer,{ |numChannels = 1, loop = false, type = \disk|
    var ugenTypes = IdentityDictionary[false->(disk:VDiskIn,buf:PlayBuf),true->(disk:VDiskIn,buf:PlayBufCF)]
    var playUGen = types[loop][type];
	if(loop){
			{ | bufnum, rate = 1.0, level = 1.0, i_startOffset = 0, loopTime|
			var env = EventEnv.kr;
			UOut.ar( 0, env * playUGen.ar( numChannels, bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1 ) );
		}		
		
	} {
		{ | bufnum, speed = 1.0, level = 1.0|
			var source = playUGen.ar( numChannels, bufnum, speed );
			var env = EventEnv.kr;
			UOut.ar( 0, source * env );
		}
	}
})

