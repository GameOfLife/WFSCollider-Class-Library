/*
\mouse_to_point

center: the center of the area to scale the mouse input to.
radius: the radius of the area to scale the mouse input to.
lag: a lag time to avoid zipper noise

--
this is an UMapDef definition file
part of the WFSCollider Class Library default UMapDefs set
*/

UMapDef( \mouse_to_point, { |
		center = #[0.0, 0.0], radius = #[10.0, 10.0], lag = 0.1|
	var point;
	point = [ MouseX.kr( -1, 1, lag: 0 ), MouseY.kr( -1, 1, lag: 0 ).neg ];
	point = point.lag3( lag );
	radius = radius.max(1.0e-10);
	point = point.linlin( -1, 1, radius.neg, radius, \none ) + center;
	UMapOut.kr(point, false);
})
	.setSpec( \center, WFSPointSpec(200) )
	.setSpec( \radius, WFSRadiusSpec(Rect(0,0,200,200)) )
	.setSpec( \lag, [0,1].asSpec )
	.category_( 'point_convert' )
	.canUseUMapFunc_({ |unit, key, umapdef|
		unit.getSpec( key ).isKindOf( PointSpec );
	})
