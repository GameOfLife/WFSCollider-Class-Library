

EQArg {
    var <>values; // i.e. [[100,0,1], [250,0,1], [1000,0,1], [3500,0,1], [6000,0,1]]

    *new{ |values|^super.newCopyArgs(values) }

	asControlInputFor { ^this.toSynthArg }

	toSynthArg{ ^values.collect({ |item,i|
	    [ item[0].cpsmidi - 1000.cpsmidi, item[1], item[2].log10 ] }).flat
    }

	printOn { arg stream;
		stream << "a " << this.class.name << "(" <<*[
		    values]
		<<")"
	}

    storeOn { arg stream;
		stream << this.class.name << "(" <<*[
		    values]
		<<")"
	}
}