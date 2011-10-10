Udef( \tremolo, { |speed = 1, amount = #[0.0,1.0], smooth = 1|
	UOut.ar( 0, SinOsc.kr( speed ).pow(smooth).range(*amount) * UIn.ar( 0 ) )
} )
	.category_( \effect )
	.setSpec( \speed, [0.125, 8, 'exp', 0, 1].asSpec )
	.setSpec( \smooth, [0,1,\lin, 0, 1] );