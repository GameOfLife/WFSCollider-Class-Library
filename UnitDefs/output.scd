Udef( \output, { |bus = 0|
	Out.ar( bus, UIn.ar( 0 ) );
} ).setSpec( \bus, [0,7,\lin,1] )