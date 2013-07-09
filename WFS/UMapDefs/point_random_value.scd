FuncUMapDef( \point_random_value, { |
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