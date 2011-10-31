Udef( \output, { |bus = 0|
	Out.ar( bus, UGlobalEQ.ar( UIn.ar( 0 ) ) * UEnv.kr );
} ).setSpec( \bus, PositiveIntegerSpec( 0 ) )
	.shouldPlayOnFunc_({ |unit, target|
		Server.default == target.asTarget.server; // safety measure: no output on wfs servers
	})
	.category_( \io );