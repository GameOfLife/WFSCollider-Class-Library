UMapDef( \point_random, { |
		time = #[1,1],
		center = #[0.5,0.5],
		radius = #[0.525,0.525],
		lag = 1,
		seed = 12345|
	var random, trigger;
	URandSeed.ir(seed);
	radius = radius.linlin(0,1,-1,1);
	center = center.linlin(0,1,-1,1);
	trigger = TDuty.kr( Dwhite(0,1).linexp(0,1,*time) );
	random = [
		Demand.kr( trigger, 0, Dwhite(-1,1)).madd( radius[0], center[0] ),
		Demand.kr( trigger, 0, Dwhite(-1,1)).madd( radius[1], center[1] )
	]; 
	random = LPFLag.kr( random, lag );
	random = random.linlin(-1,1,0,1);
	UMapOut.kr( random );
})
	.category_( 'point' )
	.setSpec( \time, RangeSpec(0.001,100, 0, inf, \exp, 0, [1,1]) )
	.setSpec( \center, WFSPointSpec(Rect(0,0,1,1)) )
	.setSpec( \radius, WFSRadiusSpec(Rect(0,0,1,1)) )
	.setSpec( \lag, [0,10,\lin,0,1].asSpec )
	.setSpec( \seed, PositiveIntegerSpec(12345) )
	.mappedArgs_([ \radius, \center ]);