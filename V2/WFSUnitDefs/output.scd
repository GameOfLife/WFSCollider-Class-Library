WFSUnitDef( \output, { |bus = 0|
	Out.ar( bus, WFSUnitIn.ar( 0 ) );
} ).setSpec( \bus, [0,7,\lin,1] )