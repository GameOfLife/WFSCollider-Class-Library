Udef( \output, { |bus = 0|
	Out.ar( bus, UIn.ar( 0 ) );
}, [ArgSpec(\bus,0,PositiveIntegerSpec())])