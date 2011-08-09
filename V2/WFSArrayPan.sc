WFSArrayConf { // configuration for one single speaker array
	
	var <>n = 48, <>dist = 5, <>angle = 0.5pi, <>offset = 0, <>spWidth;
	var <>corners;
	var <>cornerAngles; // angle to next array
	
	/*
	
	explanation of variables:
	
	angles are counter-clockwise starting at x axis:
	
	 angle 0.5pi: front array      angle 0: righthand side array
	       
	          |y(+)                               |y(+)
	          |                                   |
	          |                                   |        
	       ---+---                                |    
	x(-)      |dist   x(+)              x(-)      |   |   x(+)
	----------+----------               ----------+---+------            
	          |                                   |   | 
	          |                                   |    
	          |                                   |     
	          |y(-)                               |y(-)          
	          
	and so on:
	 	angle -0.5pi: back array
		angle pi (or -pi): lefthand side array
		
		
	
	corners are the points where two adjecent arrays cross:
	
	  array
	---------  x <- corner
	        
	           |a
	           |r
	           |r
	           |a
	           |y
	           
	           
	 each array has two corner points:
	  
	 x  -------------  x
	 1      array      2
	 
	 the cornerAngles are the angles between the adjecent arrays. They are also the area
	 where the crosfade happens when a point passes from behind one array to behind another.
	 
	 
	           |
	array      | angle, crossfade area  <- (0.5pi in this case)
	---------  x ----
	        
	           |a
	           |r
	           |r
	           |a
	           |y
	*/
	
	*new { |n = 48, dist = 5, angle = 0.5pi, offset = 0, spWidth|
		^super.newCopyArgs( n, dist, angle, offset, spWidth ? WFSBasicPan.defaultSpWidth )
			.init;
	}
	
	init {
		corners = [ dist, dist.neg ]; // assumes square setup
		cornerAngles = [ 0.5pi, 0.5pi ]; // assumes rectangular setup
	}
	
	asWFSArrayConf { ^this }
	
	adjustCorner1To { |aWFSArrayConf|
		aWFSArrayConf = aWFSArrayConf.asWFSArrayConf;
		corners[0] = aWFSArrayConf.dist; // only for square corner - TODO: other corners
		cornerAngles[0] = (angle - aWFSArrayConf.angle).wrap(-pi,pi).abs;
	}
	
	adjustCorner2To { |aWFSArrayConf|
		aWFSArrayConf = aWFSArrayConf.asWFSArrayConf;
		corners[1] = aWFSArrayConf.dist.neg; // only for square corner
		cornerAngles[1] = (angle - aWFSArrayConf.angle).wrap(-pi,pi).abs;
	}
	
	
	asArray { ^[ n, dist, angle, offset, spWidth ] }
	asCornersArray { ^(corners ++ cornerAngles); }
	
	*fromArray { |array| ^this.new( *array ); }
	
	fromCornersArray { |array| // adjust corners / angles from array
		if( array.notNil ) {
			corners = array[[0,1]];
			cornerAngles = array[[2,3]];
		};
	}
	
	asPoints { // for plotting
		^{ |i| (dist @ (i.linlin(0, n-1, spWidth / 2, spWidth.neg / 2 ) * n) - offset).rotate( angle ) } ! n;
	}
	
	asLine { // for plotting; start point and end point
		^[ 
			( dist @ ( ( spWidth * ( n /  2 ) ) - offset ) ).rotate( angle ), 
			( dist @ ( ( spWidth * ( n / -2 ) ) - offset ) ).rotate( angle ) 
		];
	}
	
	cornerPoints {
		^corners.collect({ |c|
			( dist @ c ).rotate( angle );
		});
	}
	
	draw { |mode = \lines| // 1m = 1px
		Pen.use({
			Pen.scale(1,-1);
			switch( mode,
				\lines, { 
					Pen.line( *this.asLine ).stroke 
				},
				\points, {
					this.asPoints.do({ |pt| 
						Pen.addWedge( pt, spWidth / 2, angle - 0.5pi, pi ).fill;
					}) 
			 	}
			);
		});
	}
}


WFSSpeakerConf {
	
	// a collection of WFSArrayConfs, describing a full setup
	
	var <>arrayConfs;
	
	*new { |...args|
		^super.newCopyArgs().arrayConfs_( args.collect(_.asWFSArrayConf) ).init;
	}
	
	init {
		// adjust corners and cornerAngles to each other
		arrayConfs.do({ |conf, i|
			conf.adjustCorner1To( arrayConfs.wrapAt( i-1 ) );
			conf.adjustCorner2To( arrayConfs.wrapAt( i+1 ) );
		});
	}
	
	*rect { |nx = 48, ny, dx = 5, dy|
		ny = ny ? nx;
		dy = dy ? dx;
		^this.new( [ nx, dx, 0.5pi ], [ ny, dy, 0 ], [ nx, dx, -0.5pi ], [ ny, dy, pi ] );
	}
	
	speakerCount { ^arrayConfs.collect(_.n).sum; }
	
	divideArrays { |n = 2| // split the arrayConfs into n equal (or not so equal) groups
		var division, counter = 0, result = [];
		division = this.speakerCount / n;
		n.do({ |i|
			result = result ++ [ [ ] ];
			while { (result[i].collect(_.n).sum < division) && { counter < arrayConfs.size } } {
				result[i] = result[i] ++ arrayConfs[ counter ];
				counter = counter + 1;
			};
		});
		^result;
	}
	
	getDivision { |i = 0, n = 2| // arrays for single server (server i out of n)
		^this.divideArrays(n)[i];
	} 
	
	asPoints { ^arrayConfs.collect(_.asPoints).flat }
	
	asLines { ^arrayConfs.collect(_.asLine) }
	
	draw { |mode = \lines| arrayConfs.do(_.draw(mode)); }
	
}



WFSCrossfader {
	// handles the crossfading between arrays
	var <>arrayConfs;
	var <>maxCornerAngle = pi;
	
	*new { |arrays, cornerArrays|
		
		// feed me with: 
		//   arrays: an array of WFSArrayConfs, or an array of arrays that can be 
		//           converted to WFSArrayConfs
		//   cornerArrays: arrays formatted as 
		//                 [ [ corner1, corner2, cornerAngle1, cornerAngle2 ], ...etc]
		//     - cornerArrays will override the corner settings stored in the WFSArrayConfs.
		//       Why? Because these may be provided as controls of a SynthDef
		
		arrays = arrays.collect(_.asWFSArrayConf).collect(_.copy);
		cornerArrays.do({ |item, i|
			arrays[i].fromCornersArray( item );
		});
		
		^super.newCopyArgs( arrays );
	}
	
	kr { |point = (0@0)|
		
		// will output two level values for each of the arrays: focused and normal
		
		var cornerPoints, cornerFades;
		
		cornerPoints = arrayConfs.collect( _.cornerPoints );
		
		// crossfading around corner points (normal sources)
		cornerFades = cornerPoints.collect({ |pts, i|
			var angle;
			
			angle = arrayConfs[i].angle;
			
			pts.collect({ |pt, ii|
				var pos, alpha;
				alpha = arrayConfs[i].cornerAngles[ii]; // angle towards prev / next array
				pos = (point - pt).angle - angle;
				if( ii.even ) { pos = pos.neg };
				
				// optimized corner crossfading function:
				pos = 1 - (2/pi * ((0.5 * alpha) - pos));
				pos = pos.fold2( 1 ) * (0.5pi/alpha.min(maxCornerAngle));
				((pos + 0.5).clip(0,1)).sqrt;
			}).product;

		});
		
		^cornerFades
		
	}
	
	
}

// the actual panners:

WFSBasicPan {
	
	classvar <>defaultSpWidth = 0.164;
	classvar <>speedOfSound = 344;
	classvar >sampleRate = 44100;
	
	var <>buffer, <pos;
		
	var <>maxDist = 200;
	
	maxDelay { ^( this.preDelay * 2 ) + ( ( maxDist * ( 1 - this.latencyComp ) ) / speedOfSound ) }
	bufSize { ^2 ** ( (this.maxDelay * this.sampleRate).log2.roundUp(1) ) } // next power of two
	
	// set in subclasses (can be vars too)
	preDelay { ^0 } 
	intType { ^'C' } 
	latencyComp { ^1 } 
	
	sampleRate {
		if( UGen.buildSynthDef.notNil ) { // if in a SynthDef
			^SampleRate.ir; // return actual sampleRate
		} { 
			^sampleRate
		};	
	}
	
	delay { |source, delay, amp, add = 0|
		if( UGen.buildSynthDef.notNil ) { // if in a SynthDef
			if( buffer.isNil ) { // create LocalBuf if needed
				buffer = LocalBuf( this.bufSize, 1 ).clear;
			};
					
			^("BufDelay" ++ this.intType.toUpper).asSymbol.asClass
				.ar( buffer, source, delay, amp, add );
		} { 
			^[ delay, source * amp ] // return delays and amplitudes (if not in a SynthDef)
		};	
	}
	
}


WFSPrePan : WFSBasicPan {
	
	// pre panning stage:
	/*
	- add large global delay (based on distance from center)
	- impose global amplitude roll-off
	- apply to source before WFSArrayPan
	*/
	
	var <>dbRollOff = -6; // global rolloff (relative to center of room)
	var <>limit = 2; // center radius in m (no rolloff here)
	var <>latencyComp = 0; // 1: max, 0: off 

	preDelay { ^0 } // no predelay here
	intType { ^'C' } // fixed (we might want this to be changeable?)
	
	*new { |dbRollOff = -6, limit = 2, latencyComp = 0, buffer|
		^this.newCopyArgs()
			.dbRollOff_( dbRollOff )
			.limit_( limit )
			.latencyComp_( latencyComp )
			.buffer_( buffer )
	}
	
	ar { |source, inPos, mul = 1, add = 0|
		var dist, limitAmp, amp;
		
		pos = (inPos ? pos ? (0@0)).asPoint;
		
		dist =  pos.dist( 0 @ 0 ); // distance to center
		
		limitAmp = limit.max(1).pow(dbRollOff/6); // limiting the limit to 1m to prevent large amp
		amp = dist.pow(dbRollOff/6).min( limitAmp ); // limit to prevent blowup
		
		// all together
		^this.delay( source * mul, 
			( ( dist / speedOfSound ) * (1 - latencyComp) ), 
			amp,
			add  
		);
	}
	
}


WFSArrayPan : WFSBasicPan {
	
	// point source on single array, without large delay
	
	var <n, <dist, <angle, <offset, <spWidth;
	var <intType = 'N'; // intType 'N', 'L', 'C'
	
	
	var <speakerArray; // fixed at init
	var <distances, <amplitudes, <delayTimes;
	
	var <>preDelay = 0.06; // in s (= 20m)
	
	latencyComp { ^1 }
	
	*new { |n = 48, dist = 5, angle = 0.5pi, offset = 0, spWidth, intType| // angle: 0-2pi (CCW)
		^super.newCopyArgs().init(  n, dist, angle, offset, spWidth, intType );
	}
	
	init { |inN = 48, inDist = 5, inAngle= 0.5pi, inOffset = 0, inSpWidth, inIntType|
		
		n = inN ? n; // number of speakers
		dist = inDist ? dist; // distance from center
		angle = inAngle ? angle; // angle from center
		offset = inOffset ? offset; // offset in m (to the right if angle == 0.5pi)
		spWidth = inSpWidth ? spWidth ? defaultSpWidth; // width of individual speakers		intType = inIntType ? intType ? 'N'; // better to specify in the .ar method
		
		speakerArray = { |i| (i.linlin(0, n-1, spWidth / 2, spWidth.neg / 2 ) * n) - offset } ! n;
	}

}
	
	
WFSArrayPanPoint : WFSArrayPan {
	
	/*
	// use like this:
	
	p = WFSArrayPanPoint( 48, 5, 0, \L ); // init with array specs and int type
	p.ar( source, pos ); // generate output
	
	// or:
	
	WFSArrayPanPoint( 48, 5, 0, \L ).ar( source, pos );
	
	*/
	
	var <>focus;
	var <>dbRollOff = -9; // per speaker roll-off
	var <>limit = 1; // in m
	
	ar { |source, inPos, int, mul = 1, add = 0| // inPos: Point or Polar
		var difx, dify, sqrdifx, inFront, crossing, delayOffset;
		var globalDist;

		// rotate point to array
		pos = (inPos ? pos ? (0@0)).rotate( angle.neg ).asPoint;
		
		globalDist = pos.dist( 0 @ 0 ); // distance to center
		
		// calculate distances		
		difx = pos.x - dist; // only once
		dify = pos.y - speakerArray;
		
		distances = ( difx.squared + dify.squared ).sqrt;
		
		// calculate amplitudes
		amplitudes = distances.pow(dbRollOff/6).min( limit.pow(dbRollOff/6) );
		amplitudes = amplitudes / amplitudes.sum; // normalize amps (sum == 1)
		
		// determine focus multiplier (-1 for focused or 1 for normal) 
		inFront = ((difx >= 0).binaryValue * 2) - 1;
		
		if( focus.isNil ) { 
			// auto switch (for static sources)
			delayTimes = distances * inFront;
		} {	
			// create overlapping area (for dynamic sources)
			delayTimes = ((focus-inFront) * dify.abs) + (distances * inFront);
		};
		
		delayTimes = delayTimes / speedOfSound;
		
		// subtract large delay
		delayOffset = preDelay - ( globalDist / speedOfSound );

		intType = int ? intType ? 'N';
	
		// all together
		^this.delay( source * mul, 
			delayTimes + delayOffset, 
			amplitudes,
			add  );
	}
	
}


WFSArrayPanPlane : WFSArrayPan {	
	
	var width = pi; // pi = max width
	var polarSpeakerArray;
	
	init {
		super.init;
		polarSpeakerArray = speakerArray.collect(_.asPolar);
	}
	
	ar { |source, inPos, int, mul = 1, add = 0|
		var delayOffset, angleOffsets, tanA;
		
		// rotate point to array, collect angle differences
		pos = (inPos ? pos ? (0@0)).asPolar.rotate( angle.neg ); // pos becomes polar
		//angleOffsets = speakerArray.collect({ |item, i| pos.theta - item.theta; });
		
		// calculate distances
		
		distances = polarSpeakerArray.collect({ |item, i|  
				( ( pos.theta - item.theta ).cos * item.rho ).neg; 
				});
		
		
		//tanA = pos.theta.tan; // scientiffically correct, but tends to infinity at +0.5/-0.5pi
		//distances = speakerArray.collect({ |item,i| (tanA * item.y) + dist; });
		
		// calculate amplitudes
		amplitudes = 1;
		
		// subtract large delay
		delayOffset = preDelay + ( pos.rho / speedOfSound );
		
		intType = int ? intType ? 'N';
	
		// all together
		^this.delay( source * mul, 
			( distances / speedOfSound ) + delayOffset, 
			amplitudes,
			add );
	}
	
}


+ SequenceableCollection {
	asWFSArrayConf { ^WFSArrayConf( *this ) }
}

+ Object {
	asWFSArrayConf { ^WFSArrayConf } // default conf
}