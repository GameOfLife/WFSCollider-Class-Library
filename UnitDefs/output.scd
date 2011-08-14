Udef( \output, { |bus = 0|
	Out.ar( bus, UIn.ar( 0 ) * UEnv.kr );
} ).setSpec( \bus, PositiveIntegerSpec( 0 ) );