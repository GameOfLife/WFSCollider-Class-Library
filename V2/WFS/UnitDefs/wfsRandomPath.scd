(
// a Udef generating a circular spatial path
Udef( \wfsRandomPath, { |
		speed = 0.1, 
		center = #[0,0],
		radius = #[10,10],
		seed = 12345
		|
	var random;
	URandSeed.ir(seed);
	random = LFDNoise3.kr(  
		speed.dup,
		radius,
		center
	);
	UMixOut.kr( 0, random );
})
	.category_( 'wfs_control' )
	.setSpec( \speed, [ 0, 4, \lin, 0, 0.1 ].asSpec )
	.setSpec( \center, WFSPointSpec( 200, 0.1@0.1 ) )
	.setSpec( \radius, PointSpec( Rect(0,0,200,200), 0.1@0.1 ) )
	.setSpec( \seed, PositiveIntegerSpec(12345) )
)