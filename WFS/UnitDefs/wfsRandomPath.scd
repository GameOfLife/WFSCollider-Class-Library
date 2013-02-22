/*
\wfsRandomPath

a Udef generating a random trajectory as a control signal.

This signal can be used by the wfsDynamicPoint and wfsDynamicPlane udefs, by checking their 'pointFromBus' argument.

speed: frequency by which new random positions are generated, in Hz.center: (Point) the center of the rectangle within which a random path can be generated. radius: (Point) x and y radius of the rectangle within which the random path is generated.
lag: a smoothing time for changes in the center and radius. This uses a second-order filter, which may cause slight oscillations at beginnings and endings of movements. Changing the lag time during playback may cause audible artifacts.seed: Use this to generate a different random path. Paths with the same seed are exactly the same.

--
this is an Udef definition file
part of the WFSCollider Class Library default Udefs set
*/

Udef( \wfsRandomPath, { |
		speed = 0.1, 
		center = #[0,0],
		radius = #[10,10],
		lag = 0,
		seed = 12345
		|
	var random;
	URandSeed.ir(seed);
	random = LFDNoise3.kr(  
		speed.dup,
		LPFLag.kr( radius, lag ),
		LPFLag.kr( center, lag )
	);
	UMixOut.kr( 0, random );
})
	.category_( 'wfs_control' )
	.setSpec( \speed, [ 0, 100, 99.squared.log, 0, 0.1 ].asSpec )
	.setSpec( \center, WFSPointSpec( 200, 0.1@0.1 ) )
	.setSpec( \radius, PointSpec( Rect(0,0,200,200), 0.1@0.1 ) )
	.setSpec( \lag, [ 0, 1, \lin, 0, 0].asSpec )
	.setSpec( \seed, PositiveIntegerSpec(12345) )
	.setControlOutName( [0,1], ['x', 'y'] )