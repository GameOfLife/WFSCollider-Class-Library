/*
\line_trajectory

Creates an UMap for generating a line trajectory between two points. The points can also be UMaps.

a: the start point of the line
b: the end point of the line
curve: a curve value for the line. A 0 (zero) curve creates a linear line, a positive curve value makes the line tend towards the low value, and v.v.
duration: duration of the line in seconds
delay: delay time before the line starts

--
this is an UMapDef definition file
part of the WFSCollider Class Library default UMapDefs set
*/

UMapDef( \line_trajectory, { |
		a = #[0.0, 0.0],
		b = #[10.0,10.0],
		curve = 0, 
		duration = 10, 
		delay = 0|
	var line;
	line = UEnvGen.kr( Env([0,0,1],[delay, duration],\lin) );
	line = Select.kr( curve.inRange(-0.001,0.001), [
		line.lincurve(0,1, a, b, curve ),
		line.linlin(0,1, a, b )
	]);
	UMapOut.kr( line, false );
})
	.category_( 'point' )
	.canUseUMapFunc_({ |unit, key, umapdef|
		unit.getSpec( key ).isKindOf( PointSpec );
	})
	.setSpec( \a, WFSPointSpec() )
	.setSpec( \b, WFSPointSpec() ) 
	.setSpec( \curve, [-16,16,\lin,0,0].asSpec )
	.setSpec( \duration, SMPTESpec() )
	.setSpec( \delay, SMPTESpec() );