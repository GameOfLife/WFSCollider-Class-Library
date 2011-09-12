Udef( \output, { |bus = 0|
	Out.ar( bus, UIn.ar( 0 ) * EventEnv.kr );
}, [ArgSpec(\bus,0,PositiveIntegerSpec())])