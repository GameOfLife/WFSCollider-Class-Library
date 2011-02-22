/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2010 Wouter Snoei, Raviv Ganchrow.

    GameOfLife WFSCollider software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife WFSCollider is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife WFSCollider.  If not, see <http://www.gnu.org/licenses/>.
*/

WFSPan2D {

	classvar <>speedOfSound = 344;  // 334
	classvar <>ampType = 'tudelft';
	classvar <>maxDist = 120;
	classvar <>negativeDistance = 20;
	classvar <>defaultSpeakerSpec;
	classvar <>silent = true;
	classvar <>distanceFilterRollOff = -3;
	
*initClass { defaultSpeakerSpec = WFSConfiguration.default; }

///// ADDED TU-DELFT Amplitude calc

//// how to calculate amplitude? (both methods are dirty..)


////  this is the switching version
///  -- but nothing is done to prevent switching clicks

/// automatically changes to plane calculation when input location is a WFSPlane

*tuDelftAmp { |inDist, limit = 0|
	^inDist.pow(-1.5).min(limit.dbamp);
	}

*ravivAmp{ arg inDist, ravivFactor = 1.12; 
	// raviv's opinion  --  produces high volumes when close to speaker
	// too arbitrary?
	^(ravivFactor/inDist).squared
	}
	
*min3Amp { arg inDist, limit = 0;  
	// wouters opinion (default) - limit in db
	// -6dB / distance doubling ( 1/r )
	^(1 / inDist.sqrt.max(1 / limit.dbamp));
	}
	
*wsAmp { arg inDist, limit = 0;  
	// wouters opinion (default) - limit in db
	// -6dB / distance doubling ( 1/r )
	^inDist.max(1 / limit.dbamp).reciprocal;
	}
	
*islAmp { arg inDist, limit = 0;  
	// inverse square law - limit in db 
	// -12dB / distance doubling ( 1/r^2 )
	^inDist.squared.max(1 / limit.dbamp).reciprocal;
	}
	
*wsOldAmp{ arg inDist, limit = 0, ampFactor;  
	// wouters opinion (default) - limit in db
	// amp differences too low?
	ampFactor = ampFactor ? -10.dbamp;
	^(ampFactor/inDist.max(ampFactor/limit.dbamp));
	}

/// create a delay buffer (or more on multiple servers)
*makeBuffer { arg server, bufnum;
	var out, dur;
	server = server ? Server.local;
	dur = ( (maxDist + negativeDistance)/speedOfSound) + (256/44100); 
		// 128 extra samples for safety
	
	if( server.size == 0 )
		{ out = Buffer.alloc(server, (44100 * dur).nextPowerOfTwo, 1, 
				if( silent )
					{ nil }
					{ { |thisBuffer| ("" ++ dur.round(0.01) ++ "s delay buffer (" 
					++ thisBuffer.bufnum ++ ") created.").postln; } },
				bufnum );
		} {
		dur = (maxDist/speedOfSound) * 2;
		out = server.collect({ |oneServer|
			 Buffer.alloc(oneServer, (44100 * dur).nextPowerOfTwo, 1, 
				if( silent )
					{ nil }
					{ { |thisBuffer| ("" ++ dur.round(0.01) ++ "s delay buffer (" 
					++ thisBuffer.bufnum ++ ") created.").postln; } },
				bufnum ); 
				});
		}
	^out
	}
	
*distanceFilter { |in, location|
	var distanceFromCenter;
	distanceFromCenter = location.dist(0);
	//^BHiShelf.ar( in, min( 100000 / distanceFromCenter, 20000 ), 1, distanceFilterRollOff );
	^BHiShelf.ar( in, 10000, 1, (0.16 * distanceFromCenter).neg )
	}
	
*getDistances { arg location, speakerSpec;
	var numChannels, distArray;
	speakerSpec = speakerSpec ? defaultSpeakerSpec; 
	if( speakerSpec.class != WFSConfiguration )
		{ speakerSpec = WFSConfiguration( *speakerSpec ) };
	numChannels = speakerSpec.size; // speakerSpec = Array of WFSPoint objects
	
	//location = location ? { WFSPoint.new(0,0,0); };

	if( location.class == WFSPlane )  
		{ ^speakerSpec.distancesToPlane( location ); }
		{ ^speakerSpec.distances( location ); }
		}
		
*getSwitchDistances { arg location, speakerSpec;
	speakerSpec = speakerSpec ? defaultSpeakerSpec; 
	if( speakerSpec.class != WFSConfiguration )
		{ speakerSpec = WFSConfiguration( *speakerSpec ) };
	^speakerSpec.switchDistances( location );
	}

*getCrossFades { |location, speakerSpec, switch0, range = 0.25| // per speakerLine!!
	speakerSpec = speakerSpec ? defaultSpeakerSpec; 
	if( speakerSpec.class != WFSConfiguration )
		{ speakerSpec = WFSConfiguration( *speakerSpec ) };
		
	// crossfading full arrays depending on position
	
	// do something with switch[0] ?
	
	//if( location.class == WFSPlane )
	
	if( speakerSpec.useCrossFades )
		{ ^speakerSpec.speakerLines.collect({ |speakerLine|
			var orientation, out, range = 0.25;
			orientation = speakerLine.orientation;
			case { orientation == \h }
				{ out = ( location.y / speakerLine[0].y )
					.linlin( range.neg, range, 0,1, \minmax ); }
				{ orientation == \v }
				{ out = ( location.x / speakerLine[0].x )
					.linlin( range.neg, range, 0,1, \minmax ); }
				{ true }
				{ out = 1.0; };
			// out.dup( speakerLine.size );
			out;
			}); 
		}
		{ ^speakerSpec.speakerLines.collect({1}); };


	
	/*
	if( speakerSpec.useCrossFades )
		{ ^speakerSpec.speakerLines.collect({ |speakerLine|
			var orientation, out;
			orientation = speakerLine.orientation;
			case { orientation == \h }
				{ out = (((( location.y / speakerLine[0].y )) + 1) / 2).max(0).min(1); }
				{ orientation == \v }
				{ out = (((( location.x / speakerLine[0].x )) + 1) / 2).max(0).min(1);  }
				{ true }
				{ out = 1.0; };
			 out.dup( speakerLine.size );
			}).flat; 
		}
		{ ^1  };
	*/
	
	}
	
*getSwitchCrossFades { |location, speakerSpec, switch0, range = 0.25|
	speakerSpec = speakerSpec ? defaultSpeakerSpec; 
	if( speakerSpec.class != WFSConfiguration )
		{ speakerSpec = WFSConfiguration( *speakerSpec ) };
		
	if( speakerSpec.useCrossFades )
		{ ^speakerSpec.speakerLines.collect({ |speakerLine|
			var orientation, out, range = 0.25;
			orientation = speakerLine.orientation;
			case { orientation == \h }
				{ out = ( location.y / speakerLine[0].y )
					.linlin( range.neg, range, 0,1, \minmax ); }
				{ orientation == \v }
				{ out = ( location.x / speakerLine[0].x )
					.linlin( range.neg, range, 0,1, \minmax ); }
				{ true }
				{ out = 1.0; };
			//out.dup( speakerLine.size );
			out;
			}); 
		}
		{ ^speakerSpec.speakerLines.collect({1});   };

	
	/*
	if( speakerSpec.useCrossFades )
		{ ^speakerSpec.speakerLines.collect({ |speakerLine|
			var orientation, out;
			orientation = speakerLine.orientation;
			case { orientation == \h }
				{ out = ( location.y / speakerLine[0].y ).max(0).min(1); }
				{ orientation == \v }
				{ out = ( location.x / speakerLine[0].x ).max(0).min(1); }
				{ true }
				{ out = 1.0; };
			 out.dup( speakerLine.size );
			}).flat; 
		}
		{ ^1  };
	*/
	}

*getSwitch {  |location, speakerSpec, nonMoving = false| // and switch
	var distSqr;
	
	// [ switch (-1/1), fadeout at crosspoint ]
	//  -1 = focused, 1 = outside
	
	speakerSpec = speakerSpec ? defaultSpeakerSpec; 
	if( speakerSpec.class != WFSConfiguration )
		{ speakerSpec = WFSConfiguration( *speakerSpec ) };
	
	distSqr = speakerSpec.distanceToSquareSides( location );
	^[ ( if( speakerSpec.useSwitch && ( location.class != WFSPlane ) )
			{ ((0 < distSqr ).binaryValue * 2) - 1 }
			{ 1 } ), 
		(if( nonMoving or: { speakerSpec.useSwitch.not } )
			{ 1 }
			{ distSqr.fadeOut( 0, speakerSpec.fadeRange / 2, speakerSpec.fadeRange / 2 ) })
		];
	}
	
*getAmps { |distArray, crossFades, speakerSpec, location, switch, useFocused = true|
	var amps, allCrossfades;
	case { ampType === 'tudelft' }
			{
			if( location.class != WFSPlane )
			{ 
			amps = this.tuDelftAmp(distArray);
			amps = this.getAmpCorrection(amps, speakerSpec, location );
			
			// nieuwe versie:
			allCrossfades = crossFades 
				 * this.cornerPointCrossFades( location, speakerSpec ).max(switch.neg);
			
			/*
			// oude versie:
			allCrossfades = crossFades;
			*/			
			
			amps = amps * ( speakerSpec.speakerLines.collect({ |line,i|
					allCrossfades[i].dup( line.size )
						}).flat;
				);
				
				/*
				* this.cornerPointCrossFades( location, speakerSpec ).max(switch.neg)
				* crossFades; // lateral crossfade; same for all sources (?)
				*/
				
			if( useFocused )
				{ amps = amps * this.focusedSourceCrossFades( location, 
					speakerSpec ).max( switch ); };
				
				/*
				// cancel far away speakers for focused sources
				* ((distArray * switch) +
				(((speakerSpec.width.min( speakerSpec.depth )) / 2) + 2)).clip(0,1)
				*/
				
				
			} {
			amps = (1 / 48) * 20.dbamp * ( 2 / location.dist( WFSPoint( 0,0,0 ) ) ).min(1)
				 //this.getAmpCorrection( speakerSpec, location )
				 // * crossFades;
				  * speakerSpec.speakerLines.collect({ |line,i|
					crossFades[i].dup( line.size )
						}).flat;
				
			};
		 }
		{ ampType === 'ws' }
			{amps = this.wsAmp(distArray) * crossFades }
		{ ampType === 'raviv' }
			{amps = this.ravivAmp(distArray) }
		{ ampType === 'isl' }
			{amps = this.islAmp(distArray) }
		{ ampType === 'min3' }
			{amps = this.min3Amp(distArray) }
		{ ampType === 'none' }
			{ amps = 0.5 };
	^amps;
	}

*getAmpCorrection { |amps, speakerSpec, location| // tudelft -- only works with speakerlines!
	var radius = 2;
	^amps.clumps( speakerSpec.speakerLines.collect( _.size ) )
				.collect({|item, i|
					item * ( item.sum.reciprocal * 20.dbamp
					 * ( radius / location.dist( WFSPoint( 0,0,0 ) ) ).min(1) ); 
				   }).flat;
	/*
	^speakerSpec.speakerLines.collect({ |speakerLine|
		// correction per line
		(if( location.class != WFSPlane )
			{ speakerLine.shortestDistFrom( location ).abs
				//.max( speakerLine[0].dist( location ).sqrt )
				//.max( speakerLine.last.dist( location ).sqrt )
			.max(0.85).dup( speakerLine.size ) }
			{ 1 })
		// correction for total
		// * ( radius / location.dist( WFSPoint( 0,0,0 ) ) ).min(1);
		
		}).flat;
	*/
	}

	
*getDelayTimes { |distArray|
	^(distArray / speedOfSound) + (negativeDistance / speedOfSound);
	}


	
///// basic versions

*ar { arg sound = 0, location, speakerSpec, dcomp = 0;
	var distArray, switch; 
	distArray = this.getDistances( location, speakerSpec );
	switch = this.getSwitch(location, speakerSpec, nonMoving: false);
	^DelayL.ar(this.distanceFilter( sound, location ), 
		(maxDist / speedOfSound) * 2,	
		this.getDelayTimes( distArray * switch[0])
			- (( location.dist(0) * dcomp ) / speedOfSound ),
		this.getAmps( distArray, this.getCrossFades( location, speakerSpec, switch[0] ) ) 			* switch[1]
		
			);
	}
	
*arN { arg sound = 0, location, speakerSpec, dcomp = 0;
	var distArray, switch;
	distArray = this.getDistances( location, speakerSpec );
	switch = this.getSwitch(location, speakerSpec, nonMoving: true);
	^DelayN.ar(this.distanceFilter( sound, location ), (maxDist / speedOfSound) * 2,	
		this.getDelayTimes( distArray * switch[0])
			- (( location.dist(0) * dcomp ) / speedOfSound ),
		this.getAmps( distArray, 
			this.getCrossFades( location, speakerSpec, switch[0] ) )// no switch[1] needed
		
		);
	}
	
*arC { arg sound = 0, location, speakerSpec, dcomp = 0;
	var distArray, switch;
	distArray = this.getDistances( location, speakerSpec );
	switch = this.getSwitch(location, speakerSpec, nonMoving: false);
	^DelayC.ar(sound, (maxDist / speedOfSound) * 2,	
		this.getDelayTimes( distArray * switch[0])
			- (( location.dist(0) * dcomp ) / speedOfSound ),
		this.getAmps( distArray, this.getCrossFades( location, speakerSpec, switch[0] ) ) 
			* switch[1]
		
		);
	}
	
//// optimized versions using specified buffer as delay buffer

*basicBuf {  arg delayClass, sound = 0, bufnum, location, 
	speakerSpec, nonMoving = false, dcomp = 0, useFocused = true;
	var distArray, switch;
	distArray = this.getDistances( location, speakerSpec );
	switch = this.getSwitch(location, speakerSpec, nonMoving: nonMoving);
	
	^delayClass.ar(bufnum, this.distanceFilter( sound, location ),		this.getDelayTimes( distArray * switch[0] )
			- (( location.dist(0) * dcomp ) / speedOfSound ),
		this.getAmps( 
			distArray, 
			this.getCrossFades( location, speakerSpec, switch[0] ),
			speakerSpec, 
			location,
			switch[0],
			useFocused: useFocused
			) * switch[1]
			
		
		);
	}
	
*arBufL {  arg sound = 0, bufnum, location, speakerSpec, dcomp = 0, 
		useFocused = true;
	^this.basicBuf( BufDelayL, sound, bufnum, location, speakerSpec, dcomp: dcomp,
			useFocused: useFocused );
	}
	
*arBufN {  arg sound = 0, bufnum, location, speakerSpec, dcomp = 0, 
		useFocused = true;
	^this.basicBuf( BufDelayN, sound, bufnum, location, speakerSpec, true, 
		dcomp: dcomp, useFocused: useFocused );
	}
	
*arBufC {  arg sound = 0, bufnum, location, speakerSpec, dcomp = 0, 
		useFocused = true;

	^this.basicBuf( BufDelayC, sound, bufnum, location, speakerSpec, dcomp: dcomp, 
		useFocused: useFocused  );
	}
	
*arBufI {  arg sound = 0, bufnum, location, speakerSpec, dcomp = 0, 
		useFocused = true;

	^this.basicBuf( BufDelayN, sound, bufnum, location, speakerSpec, true, 
		dcomp: dcomp, useFocused: useFocused  );
	}

*arBuf { arg sound = 0, bufnum, location, speakerSpec, dcomp = 0, 
		useFocused = true;

	^this.arBufL( sound, bufnum, location, speakerSpec, dcomp: dcomp, 
		useFocused: useFocused  );
	}
	
// switch version (stick around speaker array and delay for distance @ 90 degrees)

*arBufSwitch { arg sound = 0, bufnum, location, 
	speakerSpec, nonMoving = false, dcomp = 0;
	var distArray, switch;
	distArray = this.getSwitchDistances( location, speakerSpec );
	switch = this.getSwitch(location, speakerSpec, nonMoving: nonMoving);
	^BufDelayL.ar(bufnum, sound,	
		this.getDelayTimes( distArray )
			- (( location.dist(0) * dcomp ) / speedOfSound ),
		this.getAmps( distArray.abs, this.getSwitchCrossFades( location, speakerSpec, switch[0] ),
			speakerSpec, 
			location, 1 /*switch[0]*/  )  
			* (1 - switch[1])
	
		);
	}

}
