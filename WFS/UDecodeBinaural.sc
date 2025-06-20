UDecodeBinaural {

	// modified copy of HOABinaural to be Unit-Lib friendly

	classvar <>midChannels;
	classvar <>sideChannels;

	*initClass {

		midChannels = #[ 0, 2, 3, 6, 7, 8, 12, 13, 14, 15, 20, 21, 22, 23, 24, 30, 31, 32, 33, 34, 35, 42, 43, 44, 45, 46, 47, 48, 56, 57, 58, 59, 60, 61, 62, 63 ];
		sideChannels = #[ 1, 4, 5, 9, 10, 11, 16, 17, 18, 19, 25, 26, 27, 28, 29, 36, 37, 38, 39, 40, 41, 49, 50, 51, 52, 53, 54, 55 ];

	}

	*ar { |in, startBuf = 0| // assumes ordered bufnums of correct order ambisonics
		var numChan, mids, sides, mid, side;
		numChan = in.size;
		mids = midChannels.select({ |item| item < numChan });
		sides = sideChannels.select({ |item| item < numChan });
		mid = mids.collect({|item| Convolution2.ar( in[item], startBuf + item, 0, 512, 1) }).sum;
		side = sides.collect({|item| Convolution2.ar( in[item], startBuf + item, 0, 512, 1) }).sum;
		^[mid + side, mid - side]
	}

	*splitBufFor { |order = 1|
		^SplitBufSndFile(
			WFSLib.filenameSymbol.asString.dirname +/+
			"resources/binauralIRs/%/irsOrd%.wav".format( ULib.sampleRate, order.asInteger )
		);
	}
}

UBinauralBufs : SplitBufSndFile {

	classvar <>irFilesPath;

	var <>order = 1;

	*initClass {
		irFilesPath = WFSLib.filenameSymbol.asString.dirname +/+ "resources/binauralIRs/%/irsOrd%.wav";
	}

	*new { |order = 1|
		^super.new( irFilesPath.format( ULib.sampleRate, order ) ).order_( order );
	}

	shallowCopy{
		^this.class.new( order );
	}


	path { ^irFilesPath.format( ULib.sampleRate, order ) }

	storeOn { arg stream;
		stream << this.class.name << "(" <<* [ order ] <<")"
	}
}