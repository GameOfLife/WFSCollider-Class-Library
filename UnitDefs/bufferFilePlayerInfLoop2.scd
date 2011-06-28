Udef( \bufferFilePlayerInfLoop2, { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_fadeOutTime = 0.1, i_startOffset = 0, loopTime, gate = 1|
		var env = EnvGen.kr( Env.new([0,1,0], 
			[ i_fadeInTime,i_fadeOutTime],releaseNode:1)
		, gate, doneAction:14); 
		UOut.ar( 0, level * env * PlayBufCF.ar(2, bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1) );
	})