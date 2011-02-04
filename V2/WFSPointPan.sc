WFSPointPan {
	
	classvar <>speedOfSound = 344;  // 334
	classvar <>negativeDistance = 20;
	classvar <>defaultSpeakerSpec;
	
	var <>source;
	var <>location;
	var <>buffer;
	var <>speakerSpec;
	
	var <>maxDistance = 120;
		
	var <>interpolation = 'linear';
	
	var <>globalAmpRollOff = -6;
	var <>globalAmpRadius = 2;
	var <>globalAmpLimit = 0;
	
	var <>perSpeakerAmpRollOff = -9;
	var <>perSpeakerRadius = 0.16;
	
	var <>fadeDistance = 0.1;
	
	var <>distanceFilterRollOff = -3;
	
	var distances, amps;

	*new { |source, location = 0, buffer, speakerSpec|
		^super.newCopyArgs( source, location, buffer, speakerSpec ).init;
		}
		
	init {
		location = location.asWFSPoint;
		speakerSpec = speakerSpec ? defaultSpeakerSpec;
		distances = nil;
		amps = nil;
		}
	
	distances { ^distances ?? { this.getDistances }; }
		
	getDistances {
		^speakerSpec.distances( location );
		}
		
	amps { ^amps ?? { this.getAmps }; }
	
	getAmps {
		
		}
		
	
	

	}

