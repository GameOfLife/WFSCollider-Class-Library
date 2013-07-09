UMapDef( \point_circle, { |
		speed = 0.1, 
		startAngle = 0,
		clockwise = 1, 
		center = #[0.5,0.5],
		radius = #[0.525,0.525],
		lag = 0,
		u_startPos = 0|
	var circle;
	radius = radius.linlin(0,1,-1,1);
	center = center.linlin(0,1,-1,1);
	circle = SinOsc.kr(  
		speed * clockwise.linlin(0,1,-1,1), 
		[ 0, 0.5pi ] + ((LPFLag.kr( startAngle, lag )) + (u_startPos * speed * 2pi).wrap(0,2pi)),
		LPFLag.kr( radius, lag ),
		LPFLag.kr( center, lag )
	);
	circle = circle.linlin(-1,1,0,1);
	UMapOut.kr( circle );
})
	.category_( 'point' )
	.setSpec( \speed, [ 0, 100, 99.squared.log, 0, 0.1 ].asSpec )
	.setSpec( \startAngle, AngleSpec() )
	.setSpec( \clockwise, BoolSpec( true ) )
	.setSpec( \center, WFSPointSpec(Rect(0,0,1,1)) )
	.setSpec( \radius, WFSRadiusSpec(Rect(0,0,1,1)) )
	.setSpec( \lag, [ 0, 1, \lin, 0, 0].asSpec )
	.mappedArgs_([ \radius, \center ]);