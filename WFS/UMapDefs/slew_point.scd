/*
\slew_point

Creates an UMap for applying a lagging filter to point input. It's main purpose is to reduce unwanted doppler shift effects.

point: the point to lag
time: the time it will take for the point to travel to a new location
linear: if true the lag will be a linear (ramp), if false (default) the lag is applied using a 3th order filter. The latter will produce less unnatural doppler shift changes.

--
this is an UMapDef definition file
part of the WFSCollider Class Library default UMapDefs set
*/

UMapDef( \slew_point, { |
		point = #[0.0, 0.0], 
		maxSpeed = 1
		|
	point = Slew.kr( point, maxSpeed, maxSpeed );
	UMapOut.kr(point, false);
})
	.setSpec( \point, WFSPointSpec(200, 0.1) )
	.setSpec( \maxSpeed, [0,344,8].asSpec )
	.category_( 'point' )
	.canUseUMapFunc_({ |unit, key, umapdef|
		unit.getSpec( key ).isKindOf( PointSpec );
	})