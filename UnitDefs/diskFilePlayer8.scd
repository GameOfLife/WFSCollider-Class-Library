Udef( \diskFilePlayer8, { | bufnum, speed = 1.0, level = 1.0, i_fadeInTime = 0.1,
		 i_duration, i_fadeOutTime = 0.1, trig| 
		var env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime, 
			(i_duration - (i_fadeInTime + i_fadeOutTime)).max(0), 
			i_fadeOutTime]), doneAction:14);
		UOut.ar( 0, level * env * level * VDiskIn.ar(8, bufnum, speed ));
	})