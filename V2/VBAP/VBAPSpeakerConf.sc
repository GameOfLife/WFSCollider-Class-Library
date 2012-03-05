VBAPSpeakerConf {
	
	classvar <>default; /* VBAPSpeakerConf */  	
	var <vbapArray;  /* VBAPSpeakerArray */
	var <distances; /* Array[Float] */
	var <buffer; /* Buffer */

	*basicNew{ |vbapArray, distances |
		^super.new.init(vbapArray, distances)
	}

	*new { |angles, distances|
		^if( angles[0].size == 0 ) {
			VBAPSpeakerConf.basicNew( VBAPSpeakerArray.new(2, angles), distances )
		} {
			VBAPSpeakerConf.basicNew( VBAPSpeakerArray.new(3, angles), distances )
		};
	}

	*changeTo { |newConfig, server|
	    VBAPSpeakerConf.default !? _.buffer !? _.free;
	    VBAPSpeakerConf.default = newConfig;
	    newConfig.makeBuffer;
	}
	
	init { |argVbapArray, argDistances|
		vbapArray = argVbapArray;
		distances = argDistances;
	}
	
	makeBuffer { |server|
		buffer = vbapArray.loadToBuffer(server);
	}
		
	*fivePointOne {
		^VBAPSpeakerConf.fromAngles( [-30, 30, 0, -110, 110] )	}
		
	*eightChan {
		^VBAPSpeakerConf.fromAngles( [0, 45, 90, 135, 180, -135, -90, -45] )
	}
	
	*zigzagDome {
		^VBAPSpeakerConf.fromAngles([ [-22.5, 14.97], [22.5, 14.97], [-67.5, 14.97], [67.5, 14.97],
		 [-112.5, 14.97], [112.5, 14.97], [-157.5, 14.97], [157.5, 14.97], [-45, 0], [45, 0], [-90, 0],
		  [90, 0], [-135, 0], [135, 0], [0, 0], [180, 0] ])
	}

	numSpeakers {
	    ^vbapArray.numSpeakers
	}
	
}