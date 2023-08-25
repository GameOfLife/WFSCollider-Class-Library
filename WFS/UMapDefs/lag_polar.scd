/*
\lag_polar

Creates an UMap for applying a lagging filter to point input. It's main purpose is to reduce unwanted doppler shift effects. This polar version converts the position to polar values before applying the lag time, and provides separate timing controls for rho and theta. The rho value has more impact on doppler shift than the theta, so it makes sense to apply more lag on that.

point: the point to lag
timeRho: the time it will take the rho/distance of the point to change to a new value
timeTheta: the time it will take the angle/theta of the point to change to a new valye
linear: if true the lag will be a linear (ramp), if false (default) the lag is applied using a 3th order filter. The latter will produce less unnatural doppler shift changes.

--
this is an UMapDef definition file
part of the WFSCollider Class Library default UMapDefs set
*/


UMapDef( \lag_polar, { |
		point = #[0.0, 0.0],
		timeRho = 0.1,
	    timeTheta = 0.1,
		linear = 0
		|
	var lin, lag;
	var polar;
	point = point.asPoint;
	polar = [ point.rho, Unwrap.kr( point.theta, -pi, pi) ];
	lin = polar.varlag( [timeRho, timeTheta] );
	lag = polar.lag3( [timeRho, timeTheta] );
	polar = LinXFade2.kr( lin, lag, linear.linlin(0,1,1,-1,\none) );
	polar = Polar( polar[0], polar[1] );
	point = [polar.real, polar.imag ];
	UMapOut.kr(point, false);
})
	.setSpec( \point, WFSPointSpec(200, 0.1) )
	.setSpec( \timeRho, [0,10, 0.1.calcCurve ].asSpec )
    .setSpec( \timeTheta, [0,10, 0.1.calcCurve ].asSpec )
	.setSpec( \linear, BoolSpec(false) )
	.category_( 'point_filter' )
	.canUseUMapFunc_({ |unit, key, umapdef|
		unit.getSpec( key ).isKindOf( PointSpec );
	})