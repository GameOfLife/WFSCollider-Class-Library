WFSPrePan {
	
	// pre panning stage:
	/*
	- add large global delay (based on distance from center)
	- impose global amplitude roll-off
	- apply to source before WFSArrayPan, if number of arrays > 1
	*/
	
	var <>dbRollOff = -6;
	var <>limit = 2; // in m
	
	*new { |buffer|
	}
	
}


WFSArrayPan {
	
	// point source on single array
	
	classvar <>defaultSpWidth = 0.164;
	classvar <>speedOfSound = 344;
	classvar <>sampleRate = 44100;
	
	var <n, <dist, <angle, <intType = 'N'; // intType 'N', 'L', 'C'
	var <>buffer, <spWidth;
	
	var <>maxDist = 200;
	
	var <speakerArray; // fixed at init
	var <point;
	
	var <distances, <amplitudes;
	var <>latencyComp = 1; // 1: max, 0: off 
	
	var <>preDelay = 0.06; // in s (= 20m)
	
	*new { |n = 48, dist = 5, angle = 0, intType, buffer, spWidth| // angle: 0-2pi CCW
		^super.newCopyArgs( n, dist, angle, intType ? 'N', buffer, spWidth ? defaultSpWidth ).init;
	}
	
	init { } // subclass responsibility
	
	delay { | source, delay, amp |
		if( UGen.buildSynthDef.notNil ) { // if in a SynthDef
			if( buffer.isNil ) { // create LocalBuf if needed
				buffer = LocalBuf( this.bufSize, 1 ).clear;
			};
					
			^("BufDelay" ++ intType.toUpper).asSymbol.asClass
				.ar( buffer, source, delay, amp );
		} { 
			^[ delay, amp ] // return delays and amplitudes (if not in a SynthDef)
		};	
	}
	
	maxDelay { ^( ( preDelay * 2 ) + ( maxDist * ( 1 - latencyComp ) ) / speedOfSound ) }
	
	bufSize { ^2 ** ( (this.maxDelay * sampleRate).log2.roundUp(1) ) } // next power of two
}
	
	
WFSArrayPanPoint : WFSArrayPan {
	
	var <focused;
	var <>dbRollOff = -9; // per speaker roll-off
	var <>limit = 1; // in m
	var <>globalDbRollOff = 0; // by default use WFSPrePan for this
	var <>globalLimit = 2; // in m
	
	init {
		speakerArray = { |i| i.linlin(0, n-1, spWidth / 2, spWidth.neg / 2 ) * n } ! n;
	}
	
	ar { |source, inPoint, inBuffer| // inPoint: Point or Polar
		var sqrdifx, delayOffset;
		var globalDist;

		globalDist = point.dist( 0 @ 0 ); // distance to center
		
		// rotate point to array
		point = (inPoint ? (0@0)).rotate( angle.neg ).asPoint;
		
		// calculate distances		
		sqrdifx = sqrdif( dist, point.x ); // x only once
		
		distances = speakerArray.collect({ |item, i|
			( sqrdifx + sqrdif( item, point.y )).sqrt;
		});
		
		// calculate amplitudes
		amplitudes = distances.pow(dbRollOff/6).min( limit.pow(dbRollOff/6) );
		amplitudes = amplitudes / amplitudes.sum; // normalize amps (sum == 1)
		
		// determine focus multiplier (-1 or 1)
		if( focused.isNil ) { // user may force
			focused = (point.x - dist).linlin(-0.01,0,-1,1, \minmax);  // in front or behind
		};
		
		// latency compensation (doppler reduction) (does this work correctly for focused?)
		delayOffset = preDelay - ( ( globalDist / speedOfSound ) * latencyComp );
		
		buffer = inBuffer;
		
		// only calculate if not 0 (non modulatable)
		if( globalDbRollOff != 0 ) {
			source = source * globalDist.max( globalLimit ).pow( globalDbRollOff / 6 );
		};
	
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
		
		// latency compensation (doppler reduction)
		delayOffset = preDelay + ( ( point.rho / speedOfSound ) * ( 1 - latencyComp ) );
			
		buffer = inBuffer;
	
		// all together
		^this.delay( source, 
			( distances / speedOfSound ) + delayOffset, 
			amplitudes );
	}
	
}