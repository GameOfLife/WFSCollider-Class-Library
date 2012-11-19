
Udef( \simpleReverb, { |time = 0.1, room = 0.5, amp = 0.2|
	var in, rev, point, dist;
	in = UIn.ar( 0, 1 );
	point = UIn.kr(0,2).asPoint;
	dist = point.rho;
	rev = FreeVerb.ar( in, 1, room, dist.linlin(0.0,8.0,0.0,1.0) ) * amp * dist.linlin(0.0,8.0,0.0,1.0);
	UOut.ar( 0, rev )
} )
	.category_( \effect )
	.setSpec( \room, [ 0, 1, \lin, 0, 0.5 ] )
	.setSpec( \amp, [0,1,\lin, 0.2], \normal )
