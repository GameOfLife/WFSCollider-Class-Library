WFSCrossfaderPt {
	
	// array crossfading for regular point sources
	// *! apply on single source, _before_ WFSArrayPan !*
	
	var <dist, <angle, <leftCorner, <rightCorner;
	var <point;
	
	
	*new { |dist = 5, angle = 0, leftCorner = -5, rightCorner|
		^super.newCopyArgs( dist, angle, leftCorner, rightCorner ? leftCorner.neg ).init;
	}
	
	init {
		// create points based on angle/dist if not provided
		if( leftCorner.isKindOf( point ).not )
			{ leftCorner = (dist @ (leftCorner ? -5)).rotate( angle ) };
		if( rightCorner.isKindOf( point ).not )
			{ rightCorner = (dist @ (rightCorner ? 5)).rotate( angle ) };
	}
	
	ar { |source = 1, inPoint, mix = 1| 
		// in fact all args can be ir, kr or ar, outputs accordingly
		var amp, leftAngle, rightAngle;
		point = inPoint;
		
		leftAngle = (point - leftCorner).theta;
		rightAngle = (point - rightCorner).theta;
		
		amp = [ 
			(((leftAngle - angle) - pi).fold(-0.25pi, 0.75pi).clip(0,0.5pi) / 0.5pi).sqrt,
			(((rightAngle - angle) + 0.5pi).fold(-0.25pi, 0.75pi).clip(0,0.5pi) / 0.5pi).sqrt
			].product;
		
		^source * ((mix * amp) + (1-mix));
	}
		
}

WFSCrossfaderFc {
	// array crossfading for focused point sources
	// *! apply on individual speakers, _after_ WFSArrayPan !*
	
	classvar <>defaultSpWidth = 0.164;
	
	var <n, <dist, <angle, <spWidth;
	var <speakerAngles, <point;
	
	var <>minWidth = 0.5pi, <>fadeRange = 0.03pi, <>radius = 2, <>offset = 0;
	
	*new { |n = 48, dist = 5, angle = 0, spWidth| // angle: 0-2pi CCW
		^super.newCopyArgs( n, dist, angle, spWidth ).init;
	}
	
	init {
		var speakerArray;
		spWidth = spWidth ? defaultSpWidth;
		speakerArray = { |i| i.linlin(0, n-1, spWidth / 2, spWidth.neg / 2 ) * n } ! n;
		speakerAngles = speakerArray.collect({ |item| (dist @ item).theta + angle; });
	}
	
	ar { |sources, inPoint, mix = 1|
		var sourceAngle, width, amps;
		
		point = inPoint;
		sourceAngle = point.theta;
		width = point.rho.linlin( offset, radius + offset, 2pi, minWidth / 2, \minmax);
		amps = speakerAngles.collect({ |speakerAngle,i|
			( speakerAngle - sourceAngle ).wrap( -pi, pi ).abs // optimization possible?
				.linlin(width, width + fadeRange, 1, 1-mix, \minmax );
		});
		
		^sources * amps;
	}

}