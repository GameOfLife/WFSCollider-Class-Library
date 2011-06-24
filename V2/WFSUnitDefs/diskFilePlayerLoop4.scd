WFSUnitDef( \diskFilePlayerLoop4, { | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
		i_eventDuration, i_fadeOutTime = 0.1, i_startOffset = 0, loopTime| 
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime,(i_eventDuration - (i_fadeInTime + i_fadeOutTime)).max(0),i_fadeOutTime])
		, doneAction:14);
		WFSUnitOut.ar( 0, env * VDiskIn.ar(4, bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1 ) );
	})