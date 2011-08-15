MetaUdef(\bufferPlayer, { |numChannels = 1|
	{ |amp = 1|
		UOut.ar( 0, BufSndFilePlayer.ar( numChannels ) * amp, true );
	}
},[\numChannels,1], [ [ \soundFile, nil, BufSndFileSpec() ] ])

/* 
MetaUdef(\bufferFilePlayer,{ |numChannels = 1, loop = false|
	if(loop){
			{ | bufnum, rate = 1.0, level = 1.0, i_fadeInTime = 0.1,
				i_fadeOutTime = 0.1, i_startOffset = 0, i_duration, loopTime| 
				var env = EnvGen.kr( Env.new([0,1,1,0], 
					[ i_fadeInTime,(i_duration - (i_fadeInTime + i_fadeOutTime)).max(0),i_fadeOutTime])
				, doneAction:14);
				UOut.ar( 0, env * PlayBufCF.ar( numChannels, bufnum, rate, Impulse.kr(rate*loopTime.reciprocal), loop:1) );
			}
		
	} {
		{ | bufnum, speed = 1.0, level = 1.0, i_fadeInTime = 0.1,
			 i_duration, i_fadeOutTime = 0.1, trig| 
			var source = PlayBuf.ar( numChannels, bufnum, speed ); 
			var env = EnvGen.kr( Env.new([0,1,1,0], 
				[ i_fadeInTime, 
				(i_duration - (i_fadeInTime + i_fadeOutTime)).max(0), 
				i_fadeOutTime]), doneAction:14);
			UOut.ar( 0, source * env);
		}
	}
},[\numChannels,1])
*/
