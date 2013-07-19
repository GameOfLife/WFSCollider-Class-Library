/*
\rotate_point

Creates an UMap intended that rotates and scales an input point.

point: the point to rotate (can be an UMap)
rotate: the amount of rotation in degrees (-pi - pi)
scale: a scale amount (0.25 - 4)
lag: a lag time for the rotate and scale values

--
this is an UMapDef definition file
part of the Unit lib default UMapDefs set
*/

UMapDef( \rotate_point, { |point = #[0.0,0.0], rotate = 0.0, scale = 1.0, lag = 0.0|
	point = point.asPoint;
	rotate = LPFLag.kr( rotate, lag );
	scale = LPFLag.kr( scale, lag );
	point = point.rotate( rotate.neg ) * [scale,scale];
	UMapOut.kr(point.asArray, false);
})
	.setSpec( \point, WFSPointSpec() )
	.setSpec( \rotate, AngleSpec() )
	.setSpec( \scale, [0.25,4,\exp,0,1].asSpec )
	.category_( 'point' )
	.canUseUMapFunc_({ |unit, key, umapdef|
		unit.getSpec( key ).isKindOf( PointSpec );
	});
