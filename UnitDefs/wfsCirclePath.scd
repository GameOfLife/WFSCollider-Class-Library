(
// a Udef generating a circular spatial path
Udef( \wfsCirclePath, { |cps = 0.125, clockwise = 1, rect = #[ -5, -5, 10, 10 ] |
	var circle, center;
	rect = rect.asRect;
	center = rect.center.asArray;
	circle = SinOsc.kr( cps * clockwise.linlin(0,1,-1,1), 
		[ 0, 0.5pi ], [ rect.width / 2, rect.height / 2 ], center 
	);
	UOut.kr( 0, circle );
})
	.category_( 'wfs_control' )
	.setDefault( \rect, Rect.aboutPoint( 0@0, 5, 5 ) )
	.setSpec( \rect, WFSRectSpec( 200 ) )
	.setSpec( \cps, [ 0, 10, \lin, 0.125, 0.125 ].asSpec )
	.setSpec( \clockwise, BoolSpec( true ) )
	.loadSynthDef( WFSServers.default.allServers );
)