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

//// OPTIMIZED FOR FLEXIBLE USE and CPU saving -- ws
//// v1.2ws

//// All calculations are in DelayL, linear interpolation, for less costly calc use DealyN
//// for hi quality results DelayC - cubic interpolation is used
//// use *arBuf for best results on / needs a buffer to be created first using WFSPan.makeBuffer!!WFSPan {

//// how to calculate amplitude? (both methods are dirty..)

*ravivAmp{ arg inDist, ravivFactor = 1.12; //raviv's opinion  --  produces high volumes when close to speaker
	^(ravivFactor/inDist).squared
	}
	
*wsAmp{ arg inDist, limit = 0, ampFactor;  // wouters opinion (default) - limit in db
	ampFactor = ampFactor ? -10.dbamp;
	^(ampFactor/inDist.max(ampFactor/limit.dbamp));
}

///// basic versions

*ar { arg sound = 0, location, speakerSpec, speedOfSound = 334, maxDist = 60, ampType = 'ws'; //'ws' or 'raviv'
	var numChannels, distArray;
	speakerSpec = speakerSpec ? [WFSPoint.new(-2.7, 1.8, 0), WFSPoint.new(2.7,1.8, 0)]; //default 2 speakers
	if( speakerSpec.class == WFSConfiguration )
		{ speakerSpec = speakerSpec.allSpeakers; };
	numChannels = speakerSpec.size; // speakerSpec = Array of WFSPoint objects
	location = location ? WFSPoint.new(0,0,0);
	distArray = Array.fill(numChannels, { |i| speakerSpec.at(i).dist(location)});
	^DelayL.ar(sound, maxDist / speedOfSound,	
		distArray / speedOfSound,
		if(ampType == 'ws')
			{WFSPan.wsAmp(distArray)}
			{WFSPan.ravivAmp(distArray)}
			);
	}
	
*arN { arg sound = 0, location, speakerSpec, speedOfSound = 334, maxDist = 60, ampType = 'ws';
	var numChannels, distArray;
	speakerSpec = speakerSpec ? [WFSPoint.new(-2.7, 1.8, 0), WFSPoint.new(2.7,1.8, 0)]; //default 2 speakers
	if( speakerSpec.class == WFSConfiguration )
		{ speakerSpec = speakerSpec.allSpeakers; };
	numChannels = speakerSpec.size; // speakerSpec = Array of WFSPoint objects
	location = location ? WFSPoint.new(0,0,0);
	distArray = Array.fill(numChannels, { |i| speakerSpec.at(i).dist(location)});
	^DelayN.ar(sound, maxDist / speedOfSound,	
		distArray / speedOfSound,
		if(ampType == 'ws')
			{WFSPan.wsAmp(distArray)}
			{WFSPan.ravivAmp(distArray)});
	}
	
*arC { arg sound = 0, location, speakerSpec, speedOfSound = 334, maxDist = 60, ampType = 'ws';
	var numChannels, distArray;
	speakerSpec = speakerSpec ? [WFSPoint.new(-2.7, 1.8, 0), WFSPoint.new(2.7,1.8, 0)]; //default 2 speakers
	if( speakerSpec.class == WFSConfiguration )
		{ speakerSpec = speakerSpec.allSpeakers; };
	numChannels = speakerSpec.size; // speakerSpec = Array of WFSPoint objects
	location = location ? WFSPoint.new(0,0,0);
	distArray = Array.fill(numChannels, { |i| speakerSpec.at(i).dist(location)});
	^DelayC.ar(sound, maxDist / speedOfSound,	
		distArray / speedOfSound,
		if(ampType == 'ws')
			{WFSPan.wsAmp(distArray)}
			{WFSPan.ravivAmp(distArray)});
	}
	
//// optimized versions using specified buffer as delay buffer
	
*arBuf { arg sound = 0, bufnum, location, speakerSpec, speedOfSound = 334, ampType = 'ws';
	var numChannels, distArray;
	speakerSpec = speakerSpec ? [WFSPoint.new(-2.7, 1.8, 0), WFSPoint.new(2.7,1.8, 0)]; //default 2 speakers
	if( speakerSpec.class == WFSConfiguration )
		{ speakerSpec = speakerSpec.allSpeakers; };
	numChannels = speakerSpec.size; // speakerSpec = Array of WFSPoint objects
	location = location ? WFSPoint.new(0,0,0);
	distArray = Array.fill(numChannels, { |i| speakerSpec.at(i).dist(location)});
	^BufDelayL.ar(bufnum, sound,	
		distArray / speedOfSound,
		if(ampType == 'ws')
			{WFSPan.wsAmp(distArray)}
			{WFSPan.ravivAmp(distArray)});
	}
	
*arBuf2D { arg sound = 0, bufnum, location, speakerSpec, speedOfSound = 334, ampType = 'ws';
	var numChannels, distArray;
	speakerSpec = speakerSpec ? [WFSPoint.new(-2.7, 1.8, 0), WFSPoint.new(2.7,1.8, 0)]; 
		//default 2 speakers
	if( speakerSpec.class == WFSConfiguration )
		{ speakerSpec = speakerSpec.allSpeakers; };
	numChannels = speakerSpec.size; // speakerSpec = Array of WFSPoint objects
	location = location ? WFSPoint.new(0,0,0);
	location.z_(0);
	distArray = Array.fill(numChannels, { |i| speakerSpec.at(i).dist(location)});
	^BufDelayL.ar(bufnum, sound,	
		distArray / speedOfSound,
		if(ampType == 'ws')
			{WFSPan.wsAmp(distArray)}
			{WFSPan.ravivAmp(distArray)});
	}
	
*arBufN { arg sound = 0, bufnum, location, speakerSpec, speedOfSound = 334, ampType = 'ws'; // no interpolation
	var numChannels, distArray;
	speakerSpec = speakerSpec ? [WFSPoint.new(-2.7, 1.8, 0), WFSPoint.new(2.7,1.8, 0)]; //default 2 speakers
	if( speakerSpec.class == WFSConfiguration )
		{ speakerSpec = speakerSpec.allSpeakers; };
	numChannels = speakerSpec.size; // speakerSpec = Array of WFSPoint objects
	location = location ? WFSPoint.new(0,0,0);
	distArray = Array.fill(numChannels, { |i| speakerSpec.at(i).dist(location)});
	^BufDelayN.ar(bufnum, sound,	
			distArray / speedOfSound,
		if(ampType == 'ws')
			{WFSPan.wsAmp(distArray)}
			{WFSPan.ravivAmp(distArray)});
	}
	
*arBufC { arg sound = 0, bufnum, location, speakerSpec, speedOfSound = 334, ampType = 'ws';  //better interpolation 
	var numChannels, distArray;
	speakerSpec = speakerSpec ? [WFSPoint.new(-2.7, 1.8, 0), WFSPoint.new(2.7,1.8, 0)]; //default 2 speakers
	if( speakerSpec.class == WFSConfiguration )
		{ speakerSpec = speakerSpec.allSpeakers; };
	numChannels = speakerSpec.size; // speakerSpec = Array of WFSPoint objects
	location = location ? WFSPoint.new(0,0,0);
	distArray = Array.fill(numChannels, { |i| speakerSpec.at(i).dist(location)});
	^BufDelayC.ar(bufnum, sound,	
		distArray / speedOfSound,
		if(ampType == 'ws')
			{WFSPan.wsAmp(distArray)}
			{WFSPan.ravivAmp(distArray)});
	}
		
*makeBuffer { arg server, bufnum, maxDist = 60, speedOfSound = 334;
	var out;
	server = server ? Server.local;
	out = Buffer.alloc(server, 44100 * (maxDist/speedOfSound), 1, 
		{ |thisBuffer| ("delay buffer (" ++ thisBuffer.bufnum ++ ") created.").postln; });
	out.addUniqueMethod('maxDist', {maxDist});  ///old style..
	out.addUniqueMethod('maxTime', {maxDist/speedOfSound});
	^out
	}
	
///// convenience methods:

*arGrid { arg sound = 0, location, width, div, offset, reverseOdd = true, speedOfSound = 334, maxDist = 60;
	^WFSPan.ar(sound, location, WFSPointArray.makeGrid(width, div, offset, reverseOdd), speedOfSound, maxDist)
}

*arGridBuf { arg sound = 0, buf, location, width, div, offset, reverseOdd = true, speedOfSound = 334;
	^WFSPan.arBuf(sound, buf, location, WFSPointArray.makeGrid(width, div, offset, reverseOdd), speedOfSound)
}

*arGridN { arg sound = 0, location, width, div, offset, reverseOdd = true, speedOfSound = 334, maxDist = 60;
	^WFSPan.arN(sound, location, WFSPointArray.makeGrid(width, div, offset, reverseOdd), speedOfSound, maxDist)
}

*arGridBufN { arg sound = 0, buf, location, width, div, offset, reverseOdd = true, speedOfSound = 334;
	^WFSPan.arBufN(sound, buf, location, WFSPointArray.makeGrid(width, div, offset, reverseOdd), speedOfSound)
}
}