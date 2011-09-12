Udef( \outputEnv, { |bus = 0|
	Out.ar( bus, UIn.ar( 0 ) * EventEnv.kr(false) );
}, [ArgSpec(\bus,0,PositiveIntegerSpec())])