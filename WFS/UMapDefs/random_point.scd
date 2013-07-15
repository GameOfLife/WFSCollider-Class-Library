/*
\random_point

Creates an UMap that generates a new random point value each time it is started. This UMap can also be used on 'init' mode parameters.

center: (Point) the center of the rectangle within which a random point can be generated. 
radius: (Point) x and y radius of the rectangle within which the random point is generated.
value: the output point (can only be changed by the UMap itself)

The 'value', 'center' and 'radius' args are mapped to that of the parameter to which the UMap is connected

--
this is an UMapDef definition file
part of the WFSCollider Class Library default UMapDefs set
*/

FuncUMapDef( \random_point, { |
		unit, 
		center = #[0.5,0.5],
		radius = #[0.525,0.525]|
	
	radius = radius.linlin(0,1,-1,1);
	center = center.linlin(0,1,-1,1);
	(center + radius.rand2).linlin(-1,1,0,1).asPoint;
})	
	.category_( 'point' )
	.numChannels_( 2 )
	.setSpec( \value, DisplaySpec( PointSpec(Rect(0,0,1,1)), { |point|
		"% @ %".format(point.x, point.y ) 
	}) )
	.setDefault( \value, 0.5@0.5 )
	.setSpec( \center, WFSPointSpec(Rect(0,0,1,1)) )
	.setSpec( \radius, WFSRadiusSpec(Rect(0,0,1,1)) )
	.mappedArgs_( [\radius, \center] );