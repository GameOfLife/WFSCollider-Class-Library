Udef( \stereoOutput, { |bus = 0, point = #[0,0], lag = 2, rollOff = (-6)|
	var sig, panned, pankr, polar, angle, amp;
	
	sig = UIn.ar( 0 );
	
	point = if( Line.kr(2,0,lag * 2).min( 1 ), point, LPF.kr( point, 1/lag ) ).asPoint;
	panned = VirtualMics.ar( [ -0.3 @ 0, 0.3 @ 0 ], sig, point, rollOff );
	polar = point.asPolar;
	angle = polar.angle.fold(0,pi).linlin(0,pi,1,-1,\none);
	amp = (0.15 / polar.rho).min( 1 );
	
	pankr = Pan2.kr( 1, angle ).max(amp);

	Out.ar( bus, UGlobalEQ.ar( panned ) * pankr * UEnv.kr  );
} )
	.category_( \io )
	.setSpec( \bus, PositiveIntegerSpec( 0 ) )
	.setSpec( \point, PointSpec( 200, step: 0.1 ) )
	.setSpec( \rollOff, [ -12, 0, \lin, 1 ].asSpec )
	.setSpec( \lag, [ 0.1, 10, \exp ].asSpec );