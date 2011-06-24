WFSUnitDef( \diskFilePlayer7, { | bufnum, speed = 1.0, level = 1.0, i_fadeInTime = 0.1,
		 i_duration, i_fadeOutTime = 0.1, trig| 
		var source = VDiskIn.ar(7, bufnum, speed );
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime, 
			(i_duration - (i_fadeInTime + i_fadeOutTime)).max(0), 
			i_fadeOutTime]), doneAction:14);
		WFSUnitOut.ar( 0, source * env );
	})