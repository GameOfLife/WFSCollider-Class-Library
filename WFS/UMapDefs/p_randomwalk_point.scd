/*
\p_randomwalk_point

Creates an pattern UMap for a circular movement of points. New values are generated each time the chain is started, or in a UPattern for each of the generated events.

speed: speed in periods per next point
startAngle: the start angle in radians
clockwise: rotate clockwise if true, counterclockwise if false
center: center of the circle
radius: radius of the circle (can be an ellipse too)

--
this is an UMapDef definition file
part of the WFSCollider Class Library default UMapDefs set
*/

UPatDef( \p_randomwalk_point, { |unit,
	    startPoint = (0@0),
	    stepSize = #[0,1],
		center = (0@0),
		radius = (10@10)|
	{
		var point, step, rradius, ccenter, scaledPoint;
		point = startPoint.next.asPoint; // only polled once
		point.yield;
		loop {
			rradius = radius.next.asPoint;
			ccenter = center.next.asPoint;
			step = stepSize.next;
			step = (step[0].asFloat rrand: step[1]) @ (step[0].asFloat rrand: step[1]);
			step = step * rradius;
			scaledPoint = ( point.x.linlin( ccenter.x - rradius.x, ccenter.x + rradius.x, 0, 1 ) ) @
		    	( point.y.linlin( ccenter.y - rradius.y, ccenter.y + rradius.y, 0, 1 ) );
			step = step * (( if( scaledPoint.x.coin ) { -1 } { 1 } ) @ ( if( scaledPoint.y.coin ) { -1 } { 1 } ));
			point = point + step;
			point.yield;
		};
	}.r
}).category_( 'pattern_point' )
	.valueIsMapped_( false )
	.numChannels_( 2 )
	.setSpec( \value, DisplaySpec( PointSpec(Rect(0,0,1,1)), { |point|
		"% @ %".format(point.x, point.y )
	}) )
	.setDefault( \value, 0.0@0.0 )
	.setDefault( \center, 0.0@0.0 )
	.setDefault( \radius, 10@10 )
    .setDefault( \startPoint, 0.0@0.0 )
    .setSpec( \stepSize, RangeSpec(0,1) )
	.setSpec( \center, WFSPointSpec(200) )
	.setSpec( \radius, WFSRadiusSpec(200) )
    .setSpec( \startPoint, WFSPointSpec(200) )
	.canUseUMapFunc_({ |unit, key, umapdef|
		var spec;
		spec = unit.getSpec( key );
		[ PointSpec, UAdaptSpec ].any({ |class| spec.isKindOf( class ); });
	});