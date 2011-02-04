WFSArrayPan {
	
	// point source on single array
	
	classvar <>defaultSpWidth = 0.164;
	classvar <>speedOfSound = 344;
	
	var <n, <dist, <angle, <intType = 'N'; // intType 'N', 'L', 'C'
	var <>buffer, <spWidth;
	
	var <speakerArray; // fixed at init
	var <point;
	
	var <distances, <amplitudes;
	var <>latencyComp = 0; // 1: max, 0: off
	
	var <>preDelay = 0.06; // in s (= 20m)
	
	*new { |n = 48, dist = 5, angle = 0, intType, buffer, spWidth| // angle: 0-2pi CCW
		^super.newCopyArgs( n, dist, angle, intType ? 'N', buffer, spWidth ).init;
	}
	
	delay { | source, delay, amp |
		if( UGen.buildSynthDef.notNil ) // if in a SynthDef
			{ 
				if( buffer.isNil ) // create LocalBuf if needed
				{  if( intType.toUpper == 'N' ) {
					buffer = LocalBuf( ( this.maxDelay * 44100 ).nextPowerOfTwo, 1 ).clear;
				} {
					buffer = LocalBuf( this.maxDelay * 44100, 1 ).clear;
				};
			};
				
			^("BufDelay" ++ intType.toUpper).asSymbol.asClass
				.ar( buffer, source, delay, amp );
		} 
		{ ^[ delay, amp ] };	// return delays and amplitudes (if not in a SynthDef)
	}
	
	maxDelay { ^( ( preDelay * 2 ) + ( 200 * ( 1 - latencyComp ) ) / speedOfSound ) }
}
	
	
WFSArrayPanPoint : WFSArrayPan {
	
	var <focused = 1;
	var <>dbRollOff = -9;
	var <>limit = 1; // in m
	
	init {
		spWidth = spWidth ? defaultSpWidth;
		speakerArray = { |i| i.linlin(0, n-1, spWidth / 2, spWidth.neg / 2 ) * n } ! n;
	}
	
	ar { |source, inPoint, inBuffer| // inPoint: Point or Polar
		var sqrdifx, delayOffset;
		
		// rotate point to array
		point = (inPoint ? (0@0)).rotate( angle.neg ).asPoint;
		
		// calculate distances		
		sqrdifx = sqrdif( dist, point.x ); // x only once
		
		distances = speakerArray.collect({ |item, i|
			( sqrdifx + sqrdif( item, point.y )).sqrt;
			});
		
		// calculate amplitudes
		amplitudes = distances.pow(dbRollOff/6).min( limit.pow(dbRollOff/6) );
		amplitudes = amplitudes / amplitudes.sum; 
		
		// determine focus multiplier (-1 or 1)
		focused = (point.x - dist).linlin(-0.01,0,-1,1, \minmax);  // in front or behind
		
		// latency compensation (doppler reduction) (does this work correctly for focused?)
		if( latencyComp != 0 )
			{ delayOffset = preDelay -
				( ( point.dist( 0 @ 0 ) / speedOfSound ) * latencyComp ) }
			{ delayOffset = preDelay };
				
		buffer = inBuffer;
	
		// all together
		^this.delay( source, 
			( ( distances / speedOfSound ) * focused) + delayOffset, 
			amplitudes );
	}
	
}


WFSArrayPanPlane : WFSArrayPan {	
	
	var width = pi; // pi = max width
	var polarSpeakerArray;
	
	init {
		spWidth = spWidth ? defaultSpWidth;
		speakerArray = { |i| dist @ ( i.linlin(0, n-1, spWidth / 2, spWidth.neg / 2 ) * n ) } ! n;
		polarSpeakerArray = speakerArray.collect(_.asPolar);
	}
	
	ar { |source, inPoint, inBuffer|
		var delayOffset, angleOffsets, tanA;
		
		// rotate point to array, collect angle differences
		point = (inPoint ? (0@0)).asPolar.rotate( angle.neg ); // point becomes polar
		//angleOffsets = speakerArray.collect({ |item, i| point.theta - item.theta; });
		
		// calculate distances
		
		distances = polarSpeakerArray.collect({ |item, i|  
				( ( point.theta - item.theta ).cos * item.rho ).neg; 
				});
		
		
		//tanA = point.theta.tan; // scientiffically correct, but tends to infinity at +0.5/-0.5pi
		//distances = speakerArray.collect({ |item,i| (tanA * item.y) + dist; });
		
		// calculate amplitudes
		amplitudes = 1;
		
		// latency compensation (doppler reduction) (does this work correctly for focused?)
		if( latencyComp != 0 )
		//if( true )
			{ delayOffset = preDelay }
			{ delayOffset = preDelay +
				( ( point.rho / speedOfSound ) * ( 1 - latencyComp ) ) 
			};
			
		buffer = inBuffer;
	
		// all together
		^this.delay( source, 
			( distances / speedOfSound ) + delayOffset, 
			amplitudes );
	}
	
}