Udef(\blipEnv,{ |rate = 1, freq = 100, noiseLevel = 0.125, blipLevel = 1,
	 i_fadeInTime = 0.1, i_duration, i_fadeOutTime = 0.1|
	var out, env;	
	RandSeed.ir( 12345 ); // always the same noise
	env = EnvGen.kr( Env.new([0,1,1,0], 
			[ i_fadeInTime, 
			(i_duration - (i_fadeInTime + i_fadeOutTime)).max(0), 
			i_fadeOutTime]), doneAction:14);

	out = ( Blip.ar( freq, 100, blipLevel * 0.125) + 
		PinkNoise.ar( noiseLevel )) * 
		LFPulse.kr(10 * rate); 
	UOut.ar(0,out * env )
})