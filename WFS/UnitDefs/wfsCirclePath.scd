/*
\wfsCirclePath

Generates a circular or elliptical control signal.

This signal can be used by the wfsDynamicPoint and wfsDynamicPlane udefs, by checking their 'pointFromBus' argument.
speed: frequency (cycles per second).startAngle: starting angle of the path in degrees.clockwise: select to turn clockwise, otherwise the path turns anti-clockwise.center: the center of the circle (Point).radius: x and y radius of the circle/ellipse (Point).

--
this is an Udef definition file
part of the WFSCollider Class Library default Udefs set
*/

Udef( \wfsCirclePath, { |
		speed = 0.1, 
		startAngle = 0,
		clockwise = 1, 
		center = #[0,0],
		radius = #[10,10],
		u_startPos = 0|
	var circle;
	circle = SinOsc.kr(  
		speed * clockwise.linlin(0,1,-1,1), 
		[ 0, 0.5pi ] + (((startAngle / 360) + (u_startPos * speed)) * 2pi),
		radius,
		center
	);
	UMixOut.kr( 0, circle );
})
	.category_( 'wfs_control' )
	.setSpec( \speed, [ 0, 100, 8, 0, 0.1 ].asSpec )
	.setSpec( \startAngle, [-180,180,\lin,1,0].asSpec )
	.setSpec( \clockwise, BoolSpec( true ) )
	.setSpec( \center, WFSPointSpec( 200, 0.1@0.1 ) )
	.setSpec( \radius, PointSpec( Rect(0,0,200,200), 0.1@0.1 ) )
	.setControlOutName( [0,1], ['x', 'y'] );