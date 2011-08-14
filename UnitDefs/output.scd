Udef( \output, { |bus = 0|
	Out.ar( bus, UIn.ar( 0 ) * UEnv.kr );
} ).setSpec( \bus, [0,96,\lin,1] )