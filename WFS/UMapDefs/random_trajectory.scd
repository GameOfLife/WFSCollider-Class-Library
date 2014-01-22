/*
\random_trajectory

Creates an UMap for generating a random trajectory for modulatable point parameters.

speed: frequency by which new random positions are generated, in Hz.
center: (Point) the center of the rectangle within which a random path can be generated. 
radius: (Point) x and y radius of the rectangle within which the random path is generated.
type: the type of noise used:
	0: step or sample-and-hold noise; hard jumps at each value change
	1: linear interpolated noise
	2: cubic interpolated noise
lag: a smoothing time for changes in the center and radius, and of the path itself if 'type' == 0. This uses a second-order filter, which may cause slight oscillations at beginnings and endings of movements. Changing the lag time during playback may cause audible artifacts.
seed: Use this to generate a different random path. Paths with the same seed are exactly the same.

--
this is an UMapDef definition file
part of the WFSCollider Class Library default UMapDefs set
*/

UMapDef( \random_trajectory, { |
		speed = 0.1,
		center = #[0.0,0.0],
		radius = #[10.0,10.0],
		type = 2,
		lag = 0.1,
		seed = 12345|
	var random, trigger;
	URandSeed.ir(seed);
	random = [
		LFDNoise0.kr( speed.dup ).lag3( lag ),
		LFDNoise1.kr( speed.dup ),
		LFDNoise3.kr( speed.dup )
	];
	random = LinSelectX.kr( type, random );
	random = random.madd( radius, center );
	UMapOut.kr( random, false );
})
	.category_( 'trajectory' )
	.canUseUMapFunc_({ |unit, key, umapdef|
		unit.getSpec( key ).isKindOf( PointSpec );
	})
	.canInsert_( false )
	.setSpec( \speed, [ 0, 100, 99.squared.log, 0, 0.1 ].asSpec )
	.setSpec( \center, WFSPointSpec(200) )
	.setSpec( \radius, WFSRadiusSpec(200) )
	.setSpec( \type, [0,2,\lin,1,0].asSpec )
	.setSpec( \lag, [0,10,\lin,0,0.1].asSpec )
	.setSpec( \seed, PositiveIntegerSpec(12345), \init );