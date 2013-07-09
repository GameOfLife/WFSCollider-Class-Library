UMapDef( \point_noise, { |
		speed = 0.1,
		center = #[0.5,0.5],
		radius = #[0.525,0.525],
		type = 2,
		lag = 0.1,
		seed = 12345|
	var random, trigger;
	URandSeed.ir(seed);
	radius = LPFLag.kr( radius, lag );
	center = LPFLag.kr( center, lag );
	random = [
		LPFLag.kr( LFDNoise0.kr( speed.dup ), lag ),
		LFDNoise1.kr( speed.dup ),
		LFDNoise3.kr( speed.dup )
	];
	random = LinSelectX.kr( type.lag(lag), random );
	random = random.madd( radius - center, center );
	UMapOut.kr( random );
})
	.category_( 'point' )
	.setSpec( \speed, [ 0, 100, 99.squared.log, 0, 0.1 ].asSpec )
	.setSpec( \center, WFSPointSpec(Rect(0,0,1,1)) )
	.setSpec( \radius, WFSRadiusSpec(Rect(0,0,1,1)) )
	.setSpec( \type, [0,2,\lin,1,0].asSpec )
	.setSpec( \lag, [0,10,\lin,0,1].asSpec )
	.setSpec( \seed, PositiveIntegerSpec(12345), \init )
	.mappedArgs_([ \radius, \center ]);