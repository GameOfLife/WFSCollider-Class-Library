(
// a Udef generating a circular spatial path
Udef( \wfsCirclePath, { |
		speed = 0.1, 
		startAngle = 0,
		clockwise = 1, 
		center = #[0,0],
		radius = #[10,10]|
	var�circle;
	circle =�SinOsc.kr(  
		speed * clockwise.linlin(0,1,-1,1), 
		[ -0.5pi, 0 ] + ((startAngle / 360) * 2pi),
		radius,
		center
	);
	UOut.kr( 0, circle );
})
	.category_( 'wfs_control' )
	.setSpec( \speed, [ 0, 4, \lin, 0, 0.1 ].asSpec )
	.setSpec( \startAngle, [-180,180,\lin,1,0].asSpec )
	.setSpec( \clockwise, BoolSpec( true ) )
	.setSpec( \center, WFSPointSpec( 200 ) )
	.setSpec( \radius, PointSpec( Rect(0,0,200,200) ) )
)