/*
\p_randomwalk_point

Creates a random walk movement for UPatterns with (WFS) Point arguments. The walk starts at 'startPoint' and goes in random steps from there. The algorithm is tuned in such a way that it gravitates to the 'center' and tries to stay within the 'radius'. If the startPoint or next point for some reason is outside the center/radius area the point will try to walk back in. center, radius and stepSize can all be changed during the walk, but startPoint is only used at the start.

startPoint: the first point. This will only be polled once per time the pattern plays
stepSize: a range from 0-1, indicating the possible step sizes relative to the radius
center: center of the area where the random walk takes place
radius: radius of the area where the random walk takes place

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