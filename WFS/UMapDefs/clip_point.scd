/*
\clip_point

Creates an UMap for applying clipping, wrapping, folding (mirroring) and more on points

point: the point to clip
center: the center of the clipping area
radius: the radius of the clipping area
clipMode: can be one of the following modes (Symbol):
	'clip' (default): clip values outside fromRange
	'fold': fold values outside fromRange
	'wrap': wrap around values outside fromRange
	'tanh': distort values to range with a tanh function
	'softclip': softclip values outside fromRange
	'distort': use a non-linear distortion curve to keep all values within range
	'wrap_sine': wrap values around fromRange, and then map them to a sine curve. This means that values at the borders of fromRange will return at the center of toRange.
	'none': no clipping applied (values can go outside toRange)
lag: lag time for the center and radius values

The value arg range is mapped to that of the parameter to which the UMap is connected.

--
this is an UMapDef definition file
part of the Unit lib default UMapDefs set
*/


UMapDef( \clip_point, { |
		point = #[0.0, 0.0], 
		center = #[0.0, 0.0], 
		radius = #[10.0, 10.0], 
		clipMode = 0,
		lag = 0.0|
	center = LPFLag.kr( center, lag );
	radius = LPFLag.kr( radius, lag ).max(1.0e-10);
	point = point - center;
	point = point.linlin( radius.neg, radius, -1, 1, \none );
	point = Select.kr( clipMode, [
		point.clip2(1),
		point.fold2(1),
		point.wrap2(1),
		point.tanh,
		point.softclip,
		point.distort,
		(point.wrap2(1) * pi).sin,
		point
	]);
	point = point.linlin( -1, 1, radius.neg, radius, \none ) + center;
	UMapOut.kr(point, false);
})
	.setSpec( \point, WFSPointSpec() )
	.setSpec( \center, WFSPointSpec() )
	.setSpec( \radius, WFSRadiusSpec(Rect(0,0,200,200)) )
		.setSpec( \clipMode, ListSpec( (..7), 0, [ 
		\clip, \fold, \wrap, \tanh, \softclip, \distort, \wrap_sine, \none 
		] ) 
	)
	.category_( 'point' )
	.canUseUMapFunc_({ |unit, key, umapdef|
		unit.getSpec( key ).isKindOf( PointSpec );
	})